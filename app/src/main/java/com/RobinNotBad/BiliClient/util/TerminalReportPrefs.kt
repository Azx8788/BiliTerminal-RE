package com.RobinNotBad.BiliClient.util

/**
 * 上报开关（匿名统计 / 崩溃自动上报）的**默认值与读取落点**。
 *
 * <h3>为什么默认值收在这里</h3>
 * 与 [LongPressPrefs]、`player/SkipOpEdPrefs` 同样的理由：默认值一旦散落在多个调用点，
 * 改漏一处的症状是**「设置页显示开着、实际却是关的」**——这种错不报错、也不抛异常，
 * 只能靠人肉比对发现。本文件之前正是这种状态：`TerminalApi` 的两处读取与
 * `SettingGroupActivity` 的两处设置项各自硬编码了 `true`。
 *
 * <h3>为什么默认开</h3>
 * 这是用户拍板的取舍：默认开才能反映真实留存（唯一安装数 + 日活），代价是必须把
 * 「绝不收集任何隐私信息」那两句文案改准确——文案已同步改过。而「默认开」之上还有一道
 * 闸：真正的总闸是 [com.RobinNotBad.BiliClient.api.TerminalApi.hasPrivacyConsent]，
 * 未就当前版本隐私说明点过「同意」时一律不上报，见
 * [com.RobinNotBad.BiliClient.api.TerminalApi.isTelemetryEnabled]。
 *
 * <h3>为什么是两个常量而不是一个</h3>
 * 这是**两个独立开关**，语义不同：一个是匿名使用统计，一个是崩溃后自动上传报告。
 * 它们今天恰好默认值相同，但将来完全可能分道扬镳（例如崩溃报告上传内容更多、
 * 改判为默认关）。用一个常量硬绑在一起，会让「只想改其中一个」的人不得不拆常量，
 * 或者更糟——改了常量把另一个也悄悄改掉。两个常量各自仍是**单一来源**，
 * 既满足「默认值只定义一次」，又保留独立演化的余地。
 *
 * <h3>为什么方法名带 SwitchOn 而不是 isEnabled</h3>
 * [com.RobinNotBad.BiliClient.api.TerminalApi.isTelemetryEnabled] 是**完整判断**
 * （隐私同意闸门 + 开关），本类的方法只读**开关本身**。两者若同名，后来者很容易
 * 以为本类方法能直接回答「能不能上报」，从而绕过隐私同意这道总闸——那是隐私事故，
 * 不是风格问题。所以这里刻意用 `SwitchOn` 把语义钉死在「开关存档值」上。
 *
 * <h3>读取语义</h3>
 * [SharedPreferencesUtil.getBoolean] 的语义是「**键不存在才用默认值**」，
 * 所以老用户显式关过之后不会被默认值重新打开。
 */
object TerminalReportPrefs {

    /** [SettingsKeys.TELEMETRY_ENABLE] 的默认值：开启（匿名使用统计）。 */
    const val DEFAULT_TELEMETRY_ENABLED = true

    /** [SettingsKeys.CRASH_REPORT_AUTO] 的默认值：开启（崩溃后自动上传报告）。 */
    const val DEFAULT_CRASH_REPORT_ENABLED = true

    /**
     * 匿名统计**开关本身的存档值**（不含隐私同意这道闸）。
     *
     * 只读 [SettingsKeys.TELEMETRY_ENABLE]；「必须先同意隐私说明」由
     * [com.RobinNotBad.BiliClient.api.TerminalApi.isTelemetryEnabled] 叠加，别在这里重复判。
     */
    @JvmStatic
    fun isTelemetrySwitchOn(): Boolean =
        SharedPreferencesUtil.getBoolean(SettingsKeys.TELEMETRY_ENABLE, DEFAULT_TELEMETRY_ENABLED)

    /**
     * 崩溃自动上报**开关本身的存档值**（不含隐私同意这道闸），语义同上。
     */
    @JvmStatic
    fun isCrashReportSwitchOn(): Boolean =
        SharedPreferencesUtil.getBoolean(SettingsKeys.CRASH_REPORT_AUTO, DEFAULT_CRASH_REPORT_ENABLED)
}
