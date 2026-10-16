package com.RobinNotBad.BiliClient.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.RobinNotBad.BiliClient.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 把 [Logu] 的内存日志缓冲导出成文件，并调起系统分享。
 *
 * <h3>为什么导的是「内存缓冲」而不是「落盘文件」</h3>
 * Release 包**默认不落盘**：[BiliTerminal.applyLogSwitches] 里
 * `LOG_TO_FILE = agentMode && ...`，而 `agentMode` 只在 Debug 构建下才可能为真
 * （release 里 `BuildConfig.DEBUG` 是编译期常量、被 R8 折叠），所以普通用户手里根本没有
 * `rebili.log` 这类文件可导。日志始终在内存环形缓冲里（[Logu.ringSnapshot]，只记 W/E/F，
 * 上限 64K 字符、不碰磁盘），因此这里导的就是那份内存快照——**它才是 release 下唯一存在的日志**。
 *
 * <h3>隐私</h3>
 * 缓冲里的每一行在入缓冲前已经过 `LogSanitizer` 脱敏（见 [Logu.toFile]），所以这里**不再重复脱敏**。
 *
 * <h3>线程</h3>
 * 手表端 IO 是硬约束：读快照与写文件都在 [CenterThreadPool] 的子线程里做，
 * 分享是 UI 动作、回主线程调 [Context.startActivity]。
 */
object LogExporter {

    /**
     * 导出目录名。落在应用私有 cache 下的 `terminal/` 子目录，与
     * `res/xml/file_paths.xml` 里 `<cache-path name="cache" path="terminal" />` 对应——
     * 既**不需要任何存储权限**，也正好在 FileProvider 的可授权范围内（否则 `getUriForFile` 会抛异常）。
     */
    private const val SUB_DIR = "terminal"

    /** 导出文件名前缀。 */
    private const val FILE_PREFIX = "rebili-log-"

    /**
     * 导出当前内存日志并分享。
     *
     * <p>缓冲为空时**不生成空文件**，只提示用户「暂时没有可导出的日志」。
     */
    fun export(context: Context) {
        // 读快照（内存拼接，最长 64K 字符）与写文件（磁盘 IO）都放子线程，别卡 UI。
        CenterThreadPool.run {
            val snapshot = Logu.ringSnapshot()
            if (snapshot.isEmpty()) {
                CenterThreadPool.runOnUiThread {
                    MsgUtil.showMsg(context.getString(R.string.log_export_empty))
                }
                return@run
            }
            val file = try {
                writeSnapshot(context, snapshot)
            } catch (e: Exception) {
                MsgUtil.err("export log failed", e)
                null
            }
            // 分享是 UI 动作，必须回主线程
            CenterThreadPool.runOnUiThread {
                if (file == null) {
                    MsgUtil.showMsg(context.getString(R.string.log_export_failed))
                } else {
                    share(context, file)
                }
            }
        }
    }

    /** 把快照写到应用私有 cache 下的 `terminal/` 目录，返回文件；目录建不出来时返回 null。 */
    private fun writeSnapshot(context: Context, snapshot: String): File? {
        val dir = File(context.cacheDir, SUB_DIR)
        if (!dir.exists() && !dir.mkdirs()) return null
        val name = FILE_PREFIX + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
        val file = File(dir, name)
        // Kotlin 的 writeText 默认 UTF-8，与内存缓冲里的文本一致
        file.writeText(snapshot)
        return file
    }

    /** 用 FileProvider 把文件暴露成 content:// 并调起 `Intent.createChooser`。 */
    private fun share(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".FileProvider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.log_export_subject))
                /*
                 * clipData 不是可选项，是这条链路能不能用的前提：
                 * 系统的 URI 权限只对 Intent 的 `data` 与 `clipData` 生效，而 `EXTRA_STREAM` 是 extra、
                 * **不在授权范围内**。只加 FLAG_GRANT_READ_URI_PERMISSION 的话，接收方（以及
                 * 系统 chooser 自己，uid=1000）读这个 content:// 会被拒，logcat 里是
                 *   SecurityException: Permission Denial: reading ...FileProvider uri ... 
                 *   requires the provider be exported, or grantUriPermission()
                 * 实测就是这么坏的（26.10.16），ChooserActivity 甚至直接提示要设 clipData。
                 */
                clipData = ClipData.newUri(context.contentResolver, "rebili-log", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // 逐目标显式授权：部分 ROM 的 chooser 不会把 FLAG_GRANT_READ_URI_PERMISSION 透传给被选中的应用，
            // 只靠 flag 会出现「选中后对方读不到文件」。这里照既有安装链路（DownloadActivity.installApk）的写法补一道。
            for (resolve in context.packageManager.queryIntentActivities(send, 0)) {
                context.grantUriPermission(
                    resolve.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val chooser = Intent.createChooser(send, context.getString(R.string.log_export_chooser))
            // 从非 Activity 的 Context 启动 chooser 必须带 NEW_TASK
            if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            MsgUtil.err("share log failed", e)
            MsgUtil.showMsg(context.getString(R.string.log_export_failed))
        }
    }
}
