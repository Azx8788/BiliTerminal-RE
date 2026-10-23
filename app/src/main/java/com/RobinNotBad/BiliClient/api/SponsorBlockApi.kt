package com.RobinNotBad.BiliClient.api

import com.RobinNotBad.BiliClient.player.SponsorBlockSkip
import com.RobinNotBad.BiliClient.util.Logu
import com.RobinNotBad.BiliClient.util.NetWorkUtil
import org.json.JSONArray
import java.net.URLEncoder

/**
 * bsbsb.top（BilibiliSponsorBlock 社区端口）的只读接口。
 *
 * **不要用 `NetWorkUtil.postJson` / `getJson` / `getJsonPrivacy`**：那几条会带上本机保存的
 * B 站登录 Cookie。这里统一走「自己拼头 + `NetWorkUtil.get(url, headers)`」的重载——
 * 它只把传进去的头写进请求，不注入 Cookie（实测确认，见 `NetWorkUtil.java` 的 get 重载）。
 *
 * 本接口返回 [SponsorBlockSkip.Segment] 或原始 JSON，不做 UI，解析失败一律当「没有片段」，
 * 这个功能不该因为网络问题打扰用户。
 */
object SponsorBlockApi {

    private const val BASE = "https://bsbsb.top"

    private const val VERSION = "0.1.0"

    private fun headers(): ArrayList<String> = arrayListOf(
        "User-Agent", "BiliTerminal-RE/$VERSION",
        "origin", "BiliTerminal-RE",
        "x-ext-version", VERSION
    )

    /**
     * 拉取某视频（某分P）的跳过片段原始 JSON；失败返回空串。
     *
     * `categories` 必须以 **JSON 数组** 形式传（`["sponsor","selfpromo"]`）；实测逗号分隔的
     * `sponsor,selfpromo` 会被服务端判 400。
     */
    fun fetchSkipSegments(bvid: String, cid: Long, categories: Collection<String>): String {
        if (bvid.isEmpty()) return ""
        return try {
            val categoriesJson = JSONArray(categories.toList()).toString()
            val url = BASE + "/api/skipSegments?videoID=" + bvid +
                (if (cid > 0) "&cid=" + cid else "") +
                "&categories=" + URLEncoder.encode(categoriesJson, "UTF-8")
            NetWorkUtil.get(url, headers()).use { response ->
                if (response.isSuccessful) {
                    response.body?.string() ?: ""
                } else {
                    Logu.w("跳过赞助", "HTTP " + response.code)
                    ""
                }
            }
        } catch (e: Exception) {
            Logu.e("跳过赞助", "请求失败: " + e.message)
            ""
        }
    }

    /** 上报「已看过该片段」给社区统计；fire-and-forget，失败静默。 */
    fun reportViewed(uuid: String) {
        if (uuid.isEmpty()) return
        try {
            val body = "{\"UUID\":\"" + uuid + "\"}"
            NetWorkUtil.post(BASE + "/api/viewedVideoSponsorTime", body, headers(), "application/json").use { }
        } catch (_: Exception) {
        }
    }
}