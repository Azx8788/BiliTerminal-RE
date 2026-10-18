package com.RobinNotBad.BiliClient.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.RobinNotBad.BiliClient.BiliTerminal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

//用于清除缓存，因为glide实际上会往本地存不少缩略图，时间一长就会爆炸
//清除调用我放在了每次刷新推荐页

public class FileUtil {
    public static void clearCache(Context context) {
        File cacheDir = context.getCacheDir();
        if (cacheDir.exists() && Objects.requireNonNull(cacheDir.listFiles()).length != 0)
            deleteFolder(cacheDir);
        Log.e("debug", "清除了缓存");
    }

    public static void deleteFolder(File folder) {
        if (!folder.exists()) return;

        if (folder.isFile()) {
            folder.delete();
            return;
        }

        File[] templist = folder.listFiles();
        assert templist != null;
        for (File file : templist) {
            if (file.isFile()) {   //如果该项是文件，直接删除
                file.delete();
            } else {    //如果该项是目录
                if (Objects.requireNonNull(file.listFiles()).length != 0)
                    deleteFolder(file);    //如果子文件夹不是空的，继续扫描下去，实现套娃效果
            }
            Log.e("debug", file.toString());
        }
        folder.delete();
    }

    /**
     * 只清理下载过程中的临时产物，绝不删除已下好的成品。
     *
     * 背景：下载服务退出且批次非正常结束时，此前调用 deleteFolder(整个任务目录)，
     * 而单 P 任务的目录就是 &lt;下载根&gt;/&lt;标题&gt; 本身，会把上一次成功下载好的
     * video.mp4 / audio.m4a / 封面 / 弹幕一起删掉 —— 真实数据丢失。
     *
     * 因此这里语义收紧为"只删本次下载自己产生的中间文件"：
     * - video_new.mp4 / audio_new.m4a：safeReplaceTemp 的下载临时文件；
     * - *.bak：safeReplaceTemp 替换正式文件前的备份（替换成功时已自行删除，残留说明替换被打断）；
     * - .DOWNLOADING：本任务的"下载中"标记，任务已确认为非正常结束，必须摘掉，
     *   否则本地列表会把残留的旧成品误判为"正在下载"，用户也就看不到本地缓存的视频。
     * 一旦任务目录中的上述文件都不存在（下载尚未进入写临时文件阶段就失败/退出），本方法不会留任何痕迹。
     *
     * @param folder 任务目录（即 DownloadSection.getPath() 的返回值）
     */
    public static void cleanDownloadTempFiles(File folder) {
        if (folder == null || !folder.exists()) return;
        File[] children = folder.listFiles();
        if (children == null) return;
        for (File file : children) {
            String name = file.getName();
            // 临时文件与备份文件：只删文件，不递归，避免误伤同名子目录
            if (file.isFile() && (name.endsWith("_new.mp4") || name.endsWith("_new.m4a") || name.endsWith(".bak"))) {
                file.delete();
            }
        }
        File downloadingMark = new File(folder, ".DOWNLOADING");
        if (downloadingMark.exists()) downloadingMark.delete();
    }

    public static String readString(File file) {
        if (file == null || !file.exists() || !file.canRead() || !file.isFile()) return null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            FileInputStream inputStream = new FileInputStream(file);
            FileChannel channel = inputStream.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1 << 13);
            int i;
            while ((i = channel.read(buffer)) != -1) {
                buffer.flip();
                outputStream.write(buffer.array(), 0, i);
                buffer.clear();
            }
            return outputStream.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 真的到目标目录里试写一个临时文件，用「能不能写」代替「有没有授权」。
     *
     * <p>为什么不再只看 `Environment.isExternalStorageManager()`（26.10.18 改）：
     *
     * <ul>
     *   <li>默认下载目录是 `&lt;外部存储&gt;/Android/media/&lt;包名&gt;/`，属**应用专属外部存储**，
     *       Android 10+ 写这里本来就不需要任何权限，却被授权判据一律判成「没权限」——等于每次下载都
     *       先把用户顶到系统设置页；</li>
     *   <li>手表 ROM 普遍没有「所有文件访问」设置页（issue #42：小米手表 M2505W1 / SDK 34），
     *       用户**永远授不到**这个权限，于是默认路径下的下载被永久挡死。26.10.14 只修了「点设置页崩」
     *       （见 [requestStoragePermission]），挡死的语义没有动。</li>
     * </ul>
     *
     * <p>改成试写后两种情形都对：默认目录直接放行；用户把保存路径改到公共目录、或下载图片到
     * `Pictures/` 时，试写会失败，才去要权限。API 29 及以下同理——旧运行时权限没给时试写也失败。
     *
     * @param dir 目标目录（不存在则尝试创建）
     * @return 该目录眼下是否真的能创建文件
     */
    public static boolean canWriteDirectory(File dir) {
        if (dir == null) return false;
        try {
            if (!dir.exists() && !dir.mkdirs()) return false;
            if (!dir.isDirectory()) return false;
            // 用 createTempFile 而不是固定文件名：并发下载时多个任务可能同时探测同一个根目录，
            // 固定名会互相把对方刚建的文件当成「已存在」误判成不可写。
            File probe = File.createTempFile(".write_probe", null, dir);
            boolean deleted = probe.delete();
            if (!deleted) Log.w("storage-perm", "探测文件删不掉：" + probe);
            return true;
        } catch (Throwable t) {
            // SecurityException / 只读文件系统 / 目录被并发删除 都归为「写不了」
            return false;
        }
    }

    /**
     * 保证目标目录可写：能写就直接放行，写不了才去要权限。
     *
     * @param activity 用于拉起授权页的 Activity
     * @param dir      调用方真正要写入的目录（**必须传实际目标**，别一律传下载根目录）
     * @return true 表示现在就能写；false 表示已经尝试拉起授权页/弹过提示，调用方应当停下，
     * 等用户授权后重新点一次
     */
    public static boolean ensureDirectoryWritable(Activity activity, File dir) {
        if (canWriteDirectory(dir)) return true;
        requestStoragePermission(activity);
        return false;
    }

    /**
     * 跳系统设置申请「所有文件访问权限」（Android 11+）。
     *
     * <p>26.10.14 线上崩溃：小米手表（M2505W1 / SDK 34）上 `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`
     * **没有对应的 Activity**，`startActivity` 抛 `ActivityNotFoundException`，异常冒到 `ActivityThread`
     * 把发起页整个带崩（现场：`Unable to start activity ComponentInfo{…}`）。
     * 手表 ROM 普遍不带完整设置应用，这个 action 缺失是常态，所以必须兜底而不是直接 startActivity。
     *
     * <p>三级降级：① 带包名直达本应用页面 → ② 通用的「所有文件访问」列表页 → ③ 只弹提示。
     * 详见 docs/review/fix-progress.md §3.15。
     */
    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (startSettings(activity, new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName())))) {
                return;
            }
            if (startSettings(activity, new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))) {
                return;
            }
            MsgUtil.showMsgLong("此设备打不开系统的「所有文件访问」设置页，请手动到 设置 → 应用 → 权限 里授予本应用存储权限");
        } else {
            // Android 10 及以下：请求旧存储权限
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }

    /**
     * 试着拉起一个系统设置页。
     *
     * <p>吞掉所有异常而不是只吞 `ActivityNotFoundException`：部分 ROM 该 Activity 存在但未导出，
     * 抛的是 `SecurityException`；两类都属于「这台设备去不了这个页面」，处理方式相同。
     *
     * @return 是否成功拉起
     */
    private static boolean startSettings(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.w("storage-perm", "设置页拉不起来：" + intent.getAction() + " -> " + e.getClass().getSimpleName());
            return false;
        }
    }

    public static File getVideoDownloadPath() {
        String defaultPath;
        if (BiliTerminal.context != null) {
            defaultPath = Environment.getExternalStorageDirectory() + "/Android/media/" + BiliTerminal.context.getPackageName() + "/";
        } else {
            defaultPath = Environment.getExternalStorageDirectory() + "/Android/media/com.RobinNotBad.BiliClient/";
        }
        
        File path = new File(SharedPreferencesUtil.getString("save_path_video", defaultPath));

        try {
            File nomedia = new File(path, ".nomedia");
            if (SharedPreferencesUtil.getBoolean("save_ban_gallery", true) && !nomedia.exists())
                nomedia.createNewFile();
            else if (nomedia.exists()) nomedia.delete();
        } catch (Exception ignored) {
        }
        return path;
    }

    public static File getVideoDownloadPath(String title, String child) {
        File parentFolder = new File(getVideoDownloadPath(), stringToFile(title));
        if (child == null || child.isEmpty()) return parentFolder;
        return new File(parentFolder, stringToFile(child));
    }

    public static File getPicturePath() {
        return new File(SharedPreferencesUtil.getString("save_path_pictures", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/哔哩终端/"));
    }

    public static File getDownloadPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    public static void requireTFCardPermission() {

    }

    public static String stringToFile(String str) {
        return str.substring(0, Math.min(85, str.length()))    //防止长度溢出
                .replace("|", "｜")
                .replace(":", "：")
                .replace("*", "﹡")
                .replace("?", "？")
                .replace("\"", "”")
                .replace("<", "＜")
                .replace(">", "＞")
                .replace("/", "／")
                .replace("\\", "＼");    //文件名里不能包含非法字符
    }

    public static String getFileNameFromLink(String link) {
        int length = link.length();
        for (int i = length - 1; i > 0; i--) {
            if (link.charAt(i) == '/') {
                return link.substring(i + 1);
            }
        }
        return "fail";
    }

    public static String getFileFirstName(String file) {
        for (int i = 0; i < file.length(); i++) {
            if (file.charAt(i) == '.') {
                return file.substring(0, i);
            }
        }
        return "fail";
    }

    public static boolean isVideoCached(String title) {
        File videoFolder = new File(getVideoDownloadPath(), stringToFile(title));
        if (!videoFolder.exists() || !videoFolder.isDirectory()) {
            return false;
        }
        
        File videoFile = new File(videoFolder, "video.mp4");
        File audioFile = new File(videoFolder, "audio.m4a");
        File downloadingMark = new File(videoFolder, ".DOWNLOADING");
        
        if (downloadingMark.exists()) {
            return false;
        }
        
        return videoFile.exists() || audioFile.exists();
    }
}
