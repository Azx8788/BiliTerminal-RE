package com.RobinNotBad.BiliClient.api

import com.RobinNotBad.BiliClient.model.ShortVideoItem
import org.json.JSONObject

/**
 * 竖屏短视频取流的纯逻辑：查询参数拼装、降级池判定、响应解析与跨批去重。
 *
 * 刻意不碰 Context / SharedPreferences / Build：整块逻辑因此可以直接在 JVM 单测里跑
 * （见 `StoryFeedQueryTest`），设备名、登录态这类运行时值由 [StoryFeedPager] 传进来。
 */
object StoryFeedQuery {

    /** 竖屏主信息流。 */
    const val PATH_STORY = "/x/v2/feed/index/story"

    /** 竖屏动态流：主信息流拿不到数据时的兜底（比原实现里未见于接口表的 `/x/v2/feed/index` 更可靠）。 */
    const val PATH_STORY_DYNAMIC = "/x/v2/feed/index/dynamic/story"

    /**
     * 以某条视频为锚点的相关推荐：列表刷到底、或当前视频播完且没有下一条时用它续接。
     *
     * 官方 App 的「播完自动接下一条」走的也是这条通道；响应结构与主信息流同源
     * （`data.items` / `data` 数组两种形态都做兼容，见 [parseRelated]）。
     */
    const val PATH_RELATE = "/x/v2/feed/index/relate/story"

    const val HOST = "https://app.bilibili.com"

    /**
     * `request_from` 的默认取值。
     *
     * 它是「场景来源 / 池子选择器」（0~6），不是兴趣输入：同一账号同时段不同取值会返回
     * 互不相同的内容。官方默认 0，本仓库此前完全没发这个参数，这里先对齐默认值。
     */
    const val DEFAULT_REQUEST_FROM = 0

    /**
     * 网关降级通道的 `track_id` 前缀。出现它说明这一批不是正式推荐路由，
     * 内容会明显偏向生活/搞笑这类竖屏供给充足的池子。
     */
    const val DEGRADED_TRACK_PREFIX = "gateway_fallback_"

    private const val HEX = "0123456789ABCDEF"

    /**
     * 构造一批取流的查询参数（保持插入顺序，便于日志比对）。
     *
     * 只写有依据的参数：
     * - `display_id` 批次序号，首屏 1，每成功一批 +1；
     * - `pull` 首屏 0、翻页 1（文档标注为 int，原实现传的 `true` 不在取值范围内）；
     * - `open_event` 固定 `cold`；
     * - `trackid` 回传上一批服务端下发的 `track_id`，降级通道的值不回传；
     * - **不发** `from` / `from_spmid` / `spmid`：三者混传服务端直接 `-400`，单传又没有可靠取值；
     * - `request_from` 场景/池选择器，默认 [DEFAULT_REQUEST_FROM]，取值会被收敛到 0..6。
     *
     * @param displayId 批次序号，小于 1 时按 1 处理
     * @param firstBatch 是否为本次会话的首屏
     * @param trackId 上一批的 `track_id`，可为空
     * @param accessKey 登录态 access_key，未登录传空串
     * @param deviceName 设备名（Build.MODEL），取不到传空串
     * @param requestFrom 场景来源，默认 [DEFAULT_REQUEST_FROM]
     */
    fun buildParams(
        displayId: Int,
        firstBatch: Boolean,
        trackId: String?,
        accessKey: String,
        deviceName: String,
        requestFrom: Int = DEFAULT_REQUEST_FROM
    ): List<Pair<String, String>> {
        val params = ArrayList<Pair<String, String>>(20)
        params.add("mobi_app" to "android")
        params.add("platform" to "android")
        params.add("display_id" to displayId.coerceAtLeast(1).toString())
        params.add("pull" to if (firstBatch) "0" else "1")
        params.add("request_from" to requestFrom.coerceIn(0, 6).toString())
        params.add("open_event" to "cold")
        params.add("video_mode" to "2")
        params.add("voice_balance" to "1")
        params.add("s_locale" to "zh_CN")
        params.add("inline_sound" to "2")
        params.add("network" to "wifi")
        params.add("fnval" to "272")
        params.add("fnver" to "1")
        params.add("force_host" to "2")
        params.add("fourk" to "1")
        params.add("qn" to "32")
        params.add("story_mode" to "1")
        if (!trackId.isNullOrEmpty() && !isDegradedTrackId(trackId)) {
            params.add("trackid" to trackId)
        }
        if (deviceName.isNotEmpty()) {
            params.add("device_name" to deviceName)
        }
        if (accessKey.isNotEmpty()) {
            params.add("access_key" to accessKey)
        }
        return params
    }

    /**
     * 构造相关推荐请求的参数：在主信息流那套公共参数后追加锚点视频 `aid`。
     *
     * 官方 App 的相关推荐与主信息流复用同一份 QueryMap（mobi_app/platform/fnval/...），
     * 所以这里直接复用 [buildParams] 的结果而不是另拼一套，避免两处参数漂移。
     *
     * @param anchorAid 锚点视频 aid（通常是「刚播完但列表里没有下一条」的那条）
     */
    fun buildRelatedParams(
        anchorAid: Long,
        accessKey: String,
        deviceName: String
    ): List<Pair<String, String>> {
        val params = ArrayList(buildParams(1, true, null, accessKey, deviceName))
        params.add("aid" to anchorAid.toString())
        return params
    }

    /** 按 [HOST] + [path] + 查询串拼出完整 URL；参数名与值都做百分号编码。 */
    fun buildUrl(path: String, params: List<Pair<String, String>>): String {
        val builder = StringBuilder(HOST).append(path)
        if (params.isNotEmpty()) {
            builder.append('?')
            params.forEachIndexed { index, (name, value) ->
                if (index > 0) builder.append('&')
                builder.append(percentEncode(name)).append('=').append(percentEncode(value))
            }
        }
        return builder.toString()
    }

    /**
     * 百分号编码：只保留 `A-Z a-z 0-9 - _ . ~`，其余按 UTF-8 字节转 `%XX`（大写）。
     * 不用 `URLEncoder`：它会把空格编成 `+`，与接口要求的 `%20` 不一致。
     */
    fun percentEncode(value: String): String {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val builder = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val code = byte.toInt() and 0xFF
            val char = code.toChar()
            if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' ||
                char == '-' || char == '_' || char == '.' || char == '~'
            ) {
                builder.append(char)
            } else {
                builder.append('%').append(HEX[code shr 4]).append(HEX[code and 0x0F])
            }
        }
        return builder.toString()
    }

    /** 是否为网关降级通道的 `track_id`。 */
    fun isDegradedTrackId(trackId: String?): Boolean =
        !trackId.isNullOrEmpty() && trackId.startsWith(DEGRADED_TRACK_PREFIX)

    /** 成功拉取一批后批次序号 +1。 */
    fun nextDisplayId(currentDisplayId: Int): Int = currentDisplayId + 1

    /**
     * 明确的横屏卡片：拿到 `dimension` 且 `width > height` 且 `rotate == 0`。
     *
     * 缺 `dimension`、宽高非正、或 `rotate != 0`（素材旋转过）一律不判为横屏，
     * 避免字段变化时误删竖屏内容。
     */
    fun isClearlyHorizontal(item: JSONObject): Boolean {
        val dimension = item.optJSONObject("dimension") ?: return false
        val width = dimension.optInt("width", 0)
        val height = dimension.optInt("height", 0)
        if (width <= 0 || height <= 0 || width <= height) return false
        return dimension.optInt("rotate", 0) == 0
    }

    /**
     * 从服务端返回的可选清晰度里挑一个**不高于**请求值的最高档。
     *
     * playurl 的 `qn` 只是「首选」：登录态不足或稿件本身没有该清晰度时服务端会静默降级，
     * 这里按 `accept_quality` 列表还原实际能拿到的档位；列表为 null/空（部分响应不带）时原样返回请求值。
     */
    fun pickQuality(requested: Int, available: IntArray?): Int {
        if (available == null || available.isEmpty()) return requested
        var best = Int.MIN_VALUE
        var lowest = Int.MAX_VALUE
        for (q in available) {
            if (q <= 0) continue
            if (q < lowest) lowest = q
            if (q <= requested && q > best) best = q
        }
        return when {
            best != Int.MIN_VALUE -> best
            lowest != Int.MAX_VALUE -> lowest
            else -> requested
        }
    }

    /** 一批取流结果。 */
    data class FeedBatch(
        val items: List<ShortVideoItem>,
        /** 本批最后一条非空 `track_id`，用于下一批回传；全为空时为 null。 */
        val trackId: String?,
        /** 本批是否来自网关降级通道。 */
        val degraded: Boolean
    )

    val EMPTY_BATCH = FeedBatch(emptyList(), null, false)

    /**
     * 解析一批响应。
     *
     * @param root 响应 JSON，可为 null
     * @param alreadySeenAids 之前批次已经返回过的 aid（跨批去重，服务端在换批时偶尔会重复下发）
     */
    fun parseFeed(root: JSONObject?, alreadySeenAids: Set<Long> = emptySet()): FeedBatch {
        if (root == null) return EMPTY_BATCH
        if (root.optInt("code", -1) != 0) return EMPTY_BATCH
        val data = root.optJSONObject("data") ?: return EMPTY_BATCH
        val array = data.optJSONArray("items") ?: return EMPTY_BATCH

        val items = ArrayList<ShortVideoItem>(array.length())
        var trackId: String? = null
        var degraded = false
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            val itemTrackId = json.optString("track_id", "")
            if (itemTrackId.isNotEmpty()) {
                trackId = itemTrackId
                if (isDegradedTrackId(itemTrackId)) degraded = true
            }
            if (isClearlyHorizontal(json)) continue
            val item = ShortVideoItem.fromFeedItem(json) ?: continue
            if (item.aid in alreadySeenAids) continue
            if (items.any { it.aid == item.aid }) continue
            items.add(item)
        }
        return FeedBatch(items, trackId, degraded)
    }

    /**
     * 解析相关推荐响应。
     *
     * 官方响应结构未在接口文档里给全，实测存在两种形态，这里都兼容：
     * - `{"code":0,"data":{"items":[...]}}`（与主信息流同构，最常见）；
     * - `{"code":0,"data":[...]}`（直接给数组）。
     *
     * 过滤规则与主信息流一致：横屏卡片丢弃、非视频卡丢弃、按 aid 批内去重并与
     * [alreadySeenAids]（会话内 + 跨会话看过的）比对。
     */
    fun parseRelated(root: JSONObject?, alreadySeenAids: Set<Long> = emptySet()): List<ShortVideoItem> {
        if (root == null) return emptyList()
        if (root.optInt("code", -1) != 0) return emptyList()

        val array = root.optJSONArray("data")
            ?: root.optJSONObject("data")?.optJSONArray("items")
            ?: return emptyList()

        val items = ArrayList<ShortVideoItem>(array.length())
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            if (isClearlyHorizontal(json)) continue
            val item = ShortVideoItem.fromFeedItem(json) ?: continue
            if (item.aid in alreadySeenAids) continue
            if (items.any { it.aid == item.aid }) continue
            items.add(item)
        }
        return items
    }
}
