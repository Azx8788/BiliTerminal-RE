package com.RobinNotBad.BiliClient.player

import android.content.Context
import android.os.Build
import android.util.Log
import com.RobinNotBad.BiliClient.R
import com.RobinNotBad.BiliClient.api.TerminalApi
import com.RobinNotBad.BiliClient.util.CenterThreadPool
import com.RobinNotBad.BiliClient.util.CrashReporter
import com.RobinNotBad.BiliClient.util.CrashTrail
import com.RobinNotBad.BiliClient.util.Logu
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.File

/**
 * ijk 原生播放库（`libijkffmpeg.so` / `libijksdl.so` / `libijkplayer.so`）的一次性加载与失败降级。
 *
 * ## 为什么需要这个类
 *
 * `IjkMediaPlayer.loadLibrariesOnce(...)` 内部走 `System.loadLibrary`，加载失败抛的是
 * [UnsatisfiedLinkError]——它继承自 [Error] 而不是 [Exception]，所以仓库里那些
 * `catch (e: Exception)` 一律兜不住。崩溃 #48（26.10.16，共存版，Xiaomi baiji Mi Watch）就是
 * 一次 `dlopen failed: can't read file ".../lib/arm/libijksdl.so": Bad file descriptor`：
 * `loadLibrariesOnce` 在 `PlayerActivity.onCreate` 里裸调，异常直接冒到主线程，进程当场死掉。
 *
 * ## 为什么是「一次性」而不是每次重试
 *
 * `IjkMediaPlayer` 的 `mIsLibLoaded` 是静态标志，三个库全部加载成功才会置 true；
 * 只要中途失败，之后**任何一次** `IjkMediaPlayer()` 构造都会重新走 `loadLibrariesOnce`。
 * 也就是说「失败后继续用」= 每个页面都重试一遍、每次都往日志/系统里刷一遍同样的错误。
 * 因此这里一旦失败就在本进程内锁死（[STATE_FAILED]），后续调用直接返回 false、不再尝试、不再记日志，
 * 由调用方提示用户并退出播放页。
 *
 * ## 诊断信息
 *
 * 失败时把设备/系统/ABI/库目录/每个 .so 的存在性、可读性与大小写进 [Logu]（会进内存环形缓冲，
 * 崩溃报告与反馈都会附带），并在用户开着「崩溃自动上报」时上传一条崩溃报告——
 * 「Bad file descriptor」这类问题的根因在用户设备上（安装残留、存储异常、共享 uid 分包路径），
 * 本地复现不了，只能靠线上数据判断。
 */
object IjkNativeLibs {

    private const val TAG = "IjkNativeLibs"

    /** 还没尝试过加载。 */
    private const val STATE_UNLOADED = 0

    /** 三个库都加载成功，本进程内可以放心构造 [IjkMediaPlayer]。 */
    private const val STATE_LOADED = 1

    /** 本进程内加载失败过，不再重试。 */
    private const val STATE_FAILED = 2

    /** 与 `IjkMediaPlayer.loadLibrariesOnce` 里加载的三个库保持一致（顺序也一致）。 */
    private val NATIVE_LIBS = arrayOf("libijkffmpeg.so", "libijksdl.so", "libijkplayer.so")

    @Volatile
    private var loadState = STATE_UNLOADED

    @Volatile
    private var failure: Throwable? = null

    /** 失败诊断是否已经上报过（同一进程只报一次，避免滑动短视频反复上报）。 */
    @Volatile
    private var reported = false

    /**
     * 确保原生库已加载；可在主线程调用（正常路径只是一次 volatile 读）。
     *
     * 失败时返回 false 且**不会抛出任何 Throwable**，同时已经记录了诊断信息。
     * 调用方拿到 false 后必须自己走降级（提示 + 退出播放页），不要再构造 [IjkMediaPlayer]。
     *
     * @param context 仅用于失败时采集 `nativeLibraryDir` 等诊断信息与上报，可以传 null。
     */
    fun ensureLoaded(context: Context?): Boolean {
        // 快路径：加载成功后这就是一次 volatile 读，不进入同步块
        if (loadState == STATE_LOADED) return true
        if (loadState == STATE_FAILED) return false
        var ok = false
        synchronized(this) {
            if (loadState == STATE_UNLOADED) {
                // 关键点：catch 必须是 Throwable。UnsatisfiedLinkError / NoClassDefFoundError
                // 都是 Error，用 Exception 兜不住——#48 正是这样崩出去的。
                ok = try {
                    IjkMediaPlayer.loadLibrariesOnce(null)
                    loadState = STATE_LOADED
                    true
                } catch (t: Throwable) {
                    markFailed(context, t)
                    false
                }
            } else {
                // 别的线程刚做完：成功返回 true，失败返回 false（不再重试）
                ok = loadState == STATE_LOADED
            }
        }
        return ok
    }

    /** 本进程是否已经确认原生库不可用。 */
    fun isLoadFailed(): Boolean = loadState == STATE_FAILED

    /**
     * 给用户看的失败提示（已进 [R.string.msg_ijk_native_load_failed]）。
     * 取不到 Context 时退回一句等价的硬编码文本，保证任何路径都不会因为拿不到资源再崩一次。
     */
    fun failureText(context: Context?): String =
        context?.getString(R.string.msg_ijk_native_load_failed) ?: DEFAULT_FAILURE_TEXT

    /**
     * 记录「原生库本身加载成功、但构造 [IjkMediaPlayer] 时仍然抛了 Throwable」的情况。
     *
     * 构造过程里还有 `initNativeOnce()`，理论上也可能失败；调用方应在 catch 里调它，
     * 这样诊断与上报和 [ensureLoaded] 失败走同一套。
     */
    fun reportCreateFailure(context: Context?, t: Throwable) {
        markFailed(context, t)
    }

    private fun markFailed(context: Context?, t: Throwable) {
        val firstFailure = failure == null
        loadState = STATE_FAILED
        if (firstFailure) failure = t
        // 只有第一次失败时记这一行；之后 ensureLoaded 走 STATE_FAILED 快路径，不会再刷日志
        if (firstFailure) {
            Logu.e(TAG, "原生播放库加载失败（本进程不再重试）: ${t.javaClass.name}: ${t.message}")
        }
        // 目录探测是 File IO、上报是网络请求，都不能留在 Activity.onCreate 的主线程上
        CenterThreadPool.run {
            try {
                val diagnostics = buildDiagnostics(context, t)
                Logu.e(TAG, diagnostics)
                uploadOnce(context, t)
            } catch (e: Throwable) {
                Logu.e(TAG, "原生库失败诊断/上报本身出错: ${e.message}")
            }
        }
    }

    /** 是否上报由「隐私同意 + 崩溃自动上报开关」决定，判断落在 [TerminalApi.isCrashReportAutoEnabled]。 */
    private fun uploadOnce(context: Context?, t: Throwable) {
        if (reported) return
        reported = true
        val ctx = context?.applicationContext ?: return
        if (!TerminalApi.isCrashReportAutoEnabled()) {
            Logu.i(TAG, "未开启崩溃自动上报，原生库失败诊断只留在本地日志里")
            return
        }
        val result = CrashReporter.upload(
            ctx,
            t.javaClass.name + ": " + (t.message ?: ""),
            "原生播放库加载失败（已提示用户并退出播放页）",
            Thread.currentThread().name,
            Log.getStackTraceString(t),
            CrashTrail.snapshot(),
            Logu.ringSnapshot(),
            CrashReporter.uptimeSeconds()
        )
        Logu.i(TAG, "原生库失败诊断已上报，报错ID=${result.code}")
    }

    /**
     * 采集失败现场。字段刻意围绕「能不能 dlopen 到那个 .so」来选：
     * 库目录与文件的存在性/可读性/大小能区分「安装包缺库」「分区/路径异常」「文件被别的东西占了」。
     */
    private fun buildDiagnostics(context: Context?, t: Throwable): String {
        val sb = StringBuilder()
        sb.append("=== 原生播放库加载失败诊断 ===\n")
        sb.append("异常: ").append(t.javaClass.name).append(": ").append(t.message).append('\n')
        sb.append("线程: ").append(Thread.currentThread().name).append('\n')
        sb.append("设备: ").append(Build.BRAND).append(' ').append(Build.MODEL)
            .append(" / device=").append(Build.DEVICE)
            .append(" / product=").append(Build.PRODUCT).append('\n')
        sb.append("系统: Android ").append(Build.VERSION.RELEASE)
            .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
        sb.append("ABI: ").append(abiSummary()).append('\n')

        val appInfo = context?.applicationInfo
        val libDirPath = appInfo?.nativeLibraryDir
        sb.append("库目录: ").append(libDirPath ?: "（取不到 nativeLibraryDir）").append('\n')
        sb.append("安装包: ").append(appInfo?.sourceDir ?: "-").append('\n')
        if (!libDirPath.isNullOrEmpty()) {
            val dir = File(libDirPath)
            sb.append("库目录状态: 存在=").append(dir.exists())
                .append(" 可读=").append(dir.canRead())
                .append(" 可执行=").append(dir.canExecute()).append('\n')
            for (name in NATIVE_LIBS) {
                val file = File(dir, name)
                val exists = file.exists()
                sb.append("  ").append(name)
                    .append(": 存在=").append(exists)
                    .append(" 可读=").append(file.canRead())
                    .append(" 大小=").append(if (exists) file.length().toString() else "-")
                    .append('\n')
            }
            sb.append("目录内容: ").append(listLibDir(dir)).append('\n')
        }

        sb.append("原始栈:\n").append(Log.getStackTraceString(t)).append('\n')
        sb.append("=== 诊断结束 ===")
        return sb.toString()
    }

    private fun listLibDir(dir: File): String {
        return try {
            val names = dir.list()
            if (names == null) {
                "（list() 返回 null，目录不可读）"
            } else if (names.isEmpty()) {
                "（空目录）"
            } else {
                names.joinToString(", ")
            }
        } catch (e: Throwable) {
            "（读取失败: ${e.javaClass.simpleName}）"
        }
    }

    /** `Build.SUPPORTED_ABIS` 是 API 21 字段，minSdk 19 下必须先判 SDK_INT 再读。 */
    private fun abiSummary(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS?.joinToString(", ") ?: "-"
        }
        // CPU_ABI2 在部分 64 位设备上为 null，先滤掉再拼
        return listOf(Build.CPU_ABI, Build.CPU_ABI2)
            .filterNotNull()
            .filter { it.isNotEmpty() }
            .joinToString(", ")
    }

    private const val DEFAULT_FAILURE_TEXT = "播放器组件加载失败，无法播放视频"
}
