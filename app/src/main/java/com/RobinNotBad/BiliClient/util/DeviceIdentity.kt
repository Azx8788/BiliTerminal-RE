package com.RobinNotBad.BiliClient.util

import java.util.Locale
import java.util.UUID

/**
 * 设备身份：短视频取流要带的 `Display-ID` 请求头。
 *
 * 为什么需要它：官方 App 会对同一台设备发稳定的 `Display-ID`，服务端据此判断「这是不是一个
 * 真实、可识别的设备」。缺这个头时，即使已经登录（Cookie 里有 SESSDATA），内容也更容易被判进
 * `gateway_fallback_` 降级池（内容明显偏泛）。短视频取流原先只发了 UA / Referer / Cookie，
 * 这是本轮推荐改造补上的一环。
 *
 * 格式说明：B 站 App 里 `Display-ID` 的真实生成算法没有公开资料，本实现用一个**每次启动都稳定**
 * 的 32 位大写十六进制随机串代替。服务端只把它当设备标识使用，格式错误的代价是回退到旧行为，
 * 不会导致请求失败。首次取值时生成并落盘（跟随 `SharedPreferencesUtil` 的存储，`allowBackup=false`，
 * 不会外流）。
 *
 * 想确认有没有生效：看日志里取流响应下发的 `track_id`——以 `gateway_fallback_` 开头说明仍在降级池。
 */
object DeviceIdentity {

    private const val KEY_DISPLAY_ID = "short_video_display_id"

    @Volatile
    private var cached: String? = null

    /** 取本机稳定的 Display-ID；首次调用生成并持久化。 */
    fun displayId(): String {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val stored = try {
                SharedPreferencesUtil.getString(KEY_DISPLAY_ID, "")
            } catch (e: Exception) {
                ""
            }
            val value = if (stored.isNotEmpty()) {
                stored
            } else {
                val generated = UUID.randomUUID().toString()
                    .replace("-", "")
                    .uppercase(Locale.US)
                try {
                    SharedPreferencesUtil.putString(KEY_DISPLAY_ID, generated)
                    Logu.d("DeviceIdentity", "已生成 Display-ID: $generated")
                } catch (e: Exception) {
                    Logu.e("DeviceIdentity", "持久化 Display-ID 失败: ${e.message}")
                }
                generated
            }
            cached = value
            return value
        }
    }
}
