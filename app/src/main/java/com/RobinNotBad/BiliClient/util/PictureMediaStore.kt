package com.RobinNotBad.BiliClient.util

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.File

/**
 * 把已经落盘的图片登记进系统媒体库（MediaStore）。
 *
 * 为什么非做这一步不可（26.10.21 实测复现的「图片下载器下载的图片找不到」）：
 * [com.RobinNotBad.BiliClient.activity.DownloadActivity] 下载图片走的是裸 `FileOutputStream`
 * 写公共 `Pictures/哔哩终端/`，全仓**没有任何 MediaStore 写入**。而 Android 10 起
 * （本应用 targetSdk 34）用文件路径写进公共目录的文件不会被媒体库自动索引，
 * 相册/图库、以及文件管理器的「图片/相册」分类**都只查 MediaStore** ——
 * 于是文件明明躺在磁盘上（用户也看到了「下载完成」），相册和图库里却一条都没有。
 *
 * 复现与反证（手表模拟器，Android 12 / SDK 32）：
 * - 下完一张图后 `ls` 能看到 `Pictures/哔哩终端/noface.jpg`（3719 字节，内容正确）；
 * - `content query --uri content://media/external/images/media` 里**没有**这一行；
 * - 手动补一次 `MEDIA_SCANNER_SCAN_FILE` 广播，2 秒后立刻出现
 *   `_display_name=noface.jpg, relative_path=Pictures/哔哩终端/`，相册随即可见。
 * 所以这里补的就是「写完登记」这一环，不是权限问题、也不是下完被 onDestroy 删掉。
 */
object PictureMediaStore {

    /**
     * 纯函数：按文件名后缀判图片 MIME。
     *
     * 认不出来就返回 null —— 调用方据此跳过登记，免得把非图片（比如下载失败留下的
     * 报错页）当图片塞进用户相册。
     */
    @JvmStatic
    fun mimeTypeOf(fileName: String): String? =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "heic", "heif" -> "image/heic"
            "avif" -> "image/avif"
            else -> null
        }

    /**
     * 纯函数：把绝对目录裁成给人看的相对路径（toast 里用）。
     *
     * 手表屏幕窄，`/storage/emulated/0/Pictures/哔哩终端` 这种绝对路径 toast 里根本显示不全，
     * 而「`Pictures/哔哩终端`」正好是用户在文件管理器里能按图索骥的层级。
     * 不在外部存储根下（自定义路径跑到别处、或外部存储路径本身为空）就原样返回。
     */
    @JvmStatic
    fun displayFolder(folderPath: String, externalStorageRoot: String): String {
        if (externalStorageRoot.isEmpty()) return folderPath
        val root = if (externalStorageRoot.endsWith("/")) externalStorageRoot else "$externalStorageRoot/"
        return if (folderPath.startsWith(root)) folderPath.substring(root.length) else folderPath
    }

    /**
     * [displayFolder] 的便捷重载：取文件所在目录、用 `Environment` 反推外部存储根。
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    fun displayFolder(file: File): String {
        val root = try {
            Environment.getExternalStorageDirectory()?.absolutePath ?: ""
        } catch (e: Throwable) {
            // 个别设备 ROM 在存储未挂载时取根路径会抛，只是给提示文案用，不能因此崩掉下载流程
            Logu.e("PictureMediaStore", "取外部存储根失败：$e")
            ""
        }
        return displayFolder(file.parent ?: file.absolutePath, root)
    }

    /**
     * 登记一张已经写完的图片。
     *
     * 只做「尽力而为」：文件已经在磁盘上、下载也确实成功了，扫描失败最坏是相册里晚一点
     * （或系统下次空闲扫描时）才出现，**不该反过来把下载判成失败**，所以这里吞掉异常、
     * 只落日志。扫描是异步的，回调里的 uri 为 null 说明媒体库拒收了这个文件。
     */
    @JvmStatic
    fun scan(context: Context, file: File) {
        val mimeType = mimeTypeOf(file.name) ?: return
        try {
            MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), arrayOf(mimeType)
            ) { path, uri ->
                if (uri == null) Logu.e("PictureMediaStore", "媒体库登记失败：$path")
                else Logu.d("PictureMediaStore", "媒体库已登记：$path -> $uri")
            }
        } catch (e: Throwable) {
            Logu.e("PictureMediaStore", "媒体库登记异常：${file.absolutePath} $e")
        }
    }
}
