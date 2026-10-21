package com.RobinNotBad.BiliClient.api

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 竖屏短视频取流的纯函数单测：参数拼装、降级判定、响应解析。
 * 与网络无关，因此不需要 SharedPreferences / Context。
 */
class StoryFeedQueryTest {

    // ---------- 查询参数 ----------

    @Test
    fun buildParams_firstBatch_pullsZeroWithDisplayIdOne() {
        val params = StoryFeedQuery.buildParams(
            displayId = 1,
            firstBatch = true,
            trackId = null,
            accessKey = "",
            deviceName = ""
        )

        assertEquals("首屏 pull 应为 0", "0", valueOf(params, "pull"))
        assertEquals("首屏 display_id 应为 1", "1", valueOf(params, "display_id"))
        assertEquals("open_event 应为 cold", "cold", valueOf(params, "open_event"))
        assertEquals("video_mode 应为 2", "2", valueOf(params, "video_mode"))
    }

    @Test
    fun buildParams_laterBatch_pullsOneWithIncrementedDisplayId() {
        val params = StoryFeedQuery.buildParams(
            displayId = 5,
            firstBatch = false,
            trackId = "story_0.router-story-1.a.1",
            accessKey = "",
            deviceName = ""
        )

        assertEquals("翻页 pull 应为 1", "1", valueOf(params, "pull"))
        assertEquals("display_id 应透传", "5", valueOf(params, "display_id"))
    }

    @Test
    fun buildParams_displayIdBelowOne_coercedToOne() {
        val params = StoryFeedQuery.buildParams(
            displayId = 0,
            firstBatch = false,
            trackId = null,
            accessKey = "",
            deviceName = ""
        )

        assertEquals("display_id 至少为 1", "1", valueOf(params, "display_id"))
    }

    @Test
    fun buildParams_validTrackId_echoedAsTrackid() {
        val trackId = "story_0.router-story-1.a.1"
        val params = StoryFeedQuery.buildParams(2, false, trackId, "", "")

        assertEquals("正式路由的 track_id 要回传", trackId, valueOf(params, "trackid"))
    }

    @Test
    fun buildParams_degradedTrackId_dropped() {
        val params = StoryFeedQuery.buildParams(
            displayId = 2,
            firstBatch = false,
            trackId = "gateway_fallback_8b1d2f",
            accessKey = "",
            deviceName = ""
        )

        assertNull("降级通道的 track_id 不回传", valueOf(params, "trackid"))
    }

    @Test
    fun buildParams_neverSendsFromTrio() {
        val params = StoryFeedQuery.buildParams(1, true, null, "ak", "Pixel 5")
        val keys = params.map { it.first }

        assertFalse("不发 from", keys.contains("from"))
        assertFalse("不发 from_spmid", keys.contains("from_spmid"))
        assertFalse("不发 spmid", keys.contains("spmid"))
        assertEquals("access_key 应带上", "ak", valueOf(params, "access_key"))
    }

    @Test
    fun buildParams_emptyAccessKeyAndDeviceName_omitted() {
        val keys = StoryFeedQuery.buildParams(1, true, null, "", "").map { it.first }

        assertFalse("未登录不应带 access_key", keys.contains("access_key"))
        assertFalse("取不到设备名不应带 device_name", keys.contains("device_name"))
    }

    // ---------- URL 与编码 ----------

    @Test
    fun buildUrl_keepsOrderAndEncodesValues() {
        val url = StoryFeedQuery.buildUrl(
            StoryFeedQuery.PATH_STORY,
            listOf("a" to "1", "b" to "2 3")
        )

        assertEquals(
            "URL 应为 host + path + 顺序拼接的查询串",
            "https://app.bilibili.com/x/v2/feed/index/story?a=1&b=2%203",
            url
        )
    }

    @Test
    fun percentEncode_keepsUnreservedCharacters() {
        assertEquals("字母数字与 -_.~ 不编码", "aZ09-_.~", StoryFeedQuery.percentEncode("aZ09-_.~"))
        assertEquals("空格编码为 %20 而不是 +", "a%20b", StoryFeedQuery.percentEncode("a b"))
        assertEquals("斜杠应编码", "a%2Fb", StoryFeedQuery.percentEncode("a/b"))
        assertEquals(
            "中文按 UTF-8 字节大写编码",
            "%E4%B8%AD%E6%96%87",
            StoryFeedQuery.percentEncode("中文")
        )
    }

    @Test
    fun buildUrl_deviceNameWithSpace_encoded() {
        val params = StoryFeedQuery.buildParams(1, true, null, "", "Pixel 5")
        val url = StoryFeedQuery.buildUrl(StoryFeedQuery.PATH_STORY, params)

        assertTrue("设备名中的空格要编码", url.contains("device_name=Pixel%205"))
    }

    // ---------- 分页与降级 ----------

    @Test
    fun nextDisplayId_incrementsByOne() {
        assertEquals(2, StoryFeedQuery.nextDisplayId(1))
        assertEquals(11, StoryFeedQuery.nextDisplayId(10))
    }

    @Test
    fun isDegradedTrackId_matchesGatewayPrefixOnly() {
        assertTrue(StoryFeedQuery.isDegradedTrackId("gateway_fallback_abc"))
        assertFalse(StoryFeedQuery.isDegradedTrackId("story_0.router-story-1.a.1"))
        assertFalse(StoryFeedQuery.isDegradedTrackId(""))
        assertFalse(StoryFeedQuery.isDegradedTrackId(null))
    }

    // ---------- 响应解析 ----------

    @Test
    fun parseFeed_parsesVerticalCardsAndSkipsBanners() {
        val json = feedOf(
            verticalItem(1),
            """{"goto":"banner_v8","card_type":"banner_v8"}""",
            verticalItem(2)
        )

        val batch = StoryFeedQuery.parseFeed(json)

        assertEquals("只保留竖屏视频卡", 2, batch.items.size)
        assertEquals("aid 顺序应保持", listOf(1L, 2L), batch.items.map { it.aid })
        assertEquals("cid 应解析", 10L, batch.items[0].cid)
        assertEquals("标题应解析", "视频1", batch.items[0].title)
        assertFalse("正常通道不算降级", batch.degraded)
    }

    @Test
    fun parseFeed_dropsClearlyHorizontalCards() {
        val horizontal = verticalItem(2, extra = ""","dimension":{"width":1920,"height":1080,"rotate":0}""")
        val rotated = verticalItem(3, extra = ""","dimension":{"width":1920,"height":1080,"rotate":1}""")
        val vertical = verticalItem(4, extra = ""","dimension":{"width":1080,"height":1920,"rotate":0}""")

        val batch = StoryFeedQuery.parseFeed(feedOf(horizontal, rotated, vertical))

        assertEquals(
            "只丢明确横屏（rotate==0 且宽>高），rotate 非 0 与竖屏保留",
            listOf(3L, 4L),
            batch.items.map { it.aid }
        )
    }

    @Test
    fun parseFeed_missingDimension_keepsCard() {
        val batch = StoryFeedQuery.parseFeed(feedOf(verticalItem(1)))

        assertEquals("字段缺失时不要误删", 1, batch.items.size)
    }

    @Test
    fun parseFeed_dedupesWithinBatchAndAgainstSeenAids() {
        val json = feedOf(verticalItem(1), verticalItem(1), verticalItem(2), verticalItem(3))

        val batch = StoryFeedQuery.parseFeed(json, alreadySeenAids = setOf(2L))

        assertEquals("批内重复与已出现过的 aid 都要去掉", listOf(1L, 3L), batch.items.map { it.aid })
    }

    @Test
    fun parseFeed_takesLastTrackIdAndFlagsDegraded() {
        val json = feedOf(
            """{"goto":"vertical_av","player_args":{"aid":1,"cid":10},"track_id":"story_0.router-story-1.a.1"}""",
            """{"goto":"vertical_av","player_args":{"aid":2,"cid":20},"track_id":"gateway_fallback_9f8e7d"}"""
        )

        val batch = StoryFeedQuery.parseFeed(json)

        assertEquals("取最后一条非空 track_id 回传", "gateway_fallback_9f8e7d", batch.trackId)
        assertTrue("出现降级前缀应标记 degraded", batch.degraded)
    }

    @Test
    fun parseFeed_codeNotZero_returnsEmpty() {
        val json = JSONObject("""{"code":-400,"message":"请求错误","data":{}}""")

        val batch = StoryFeedQuery.parseFeed(json)

        assertTrue("接口报错应返回空批", batch.items.isEmpty())
        assertNull("报错批没有 track_id", batch.trackId)
    }

    @Test
    fun parseFeed_missingDataOrItems_returnsEmpty() {
        assertTrue("data 缺失", StoryFeedQuery.parseFeed(JSONObject("""{"code":0}""")).items.isEmpty())
        assertTrue(
            "items 缺失",
            StoryFeedQuery.parseFeed(JSONObject("""{"code":0,"data":{}}""")).items.isEmpty()
        )
        assertTrue("root 为 null", StoryFeedQuery.parseFeed(null).items.isEmpty())
    }

    @Test
    fun parseFeed_skipsCardsWithoutPlayableIds() {
        val json = feedOf(
            """{"goto":"vertical_av","title":"没有 aid"}""",
            """{"goto":"live","player_args":{"aid":9,"cid":90}}""",
            verticalItem(3)
        )

        val batch = StoryFeedQuery.parseFeed(json)

        assertEquals("没有 aid 或不是视频卡的不解析", listOf(3L), batch.items.map { it.aid })
    }

    // ---------- pickQuality：播放清晰度跟随设置 ----------

    @Test
    fun pickQuality_usesHighestNotAboveRequested() {
        val available = intArrayOf(80, 64, 32, 16)

        assertEquals("用户选 1080P 时取 80", 80, StoryFeedQuery.pickQuality(80, available))
        assertEquals("用户选 720P 时取 64", 64, StoryFeedQuery.pickQuality(64, available))
    }

    @Test
    fun pickQuality_degradesWhenRequestedMissing() {
        val available = intArrayOf(64, 32, 16)

        assertEquals("没有 1080P 时降级到列表里最高档 64", 64, StoryFeedQuery.pickQuality(80, available))
    }

    @Test
    fun pickQuality_handlesUnorderedAndBrokenList() {
        assertEquals("列表无序也按数值挑", 64, StoryFeedQuery.pickQuality(80, intArrayOf(16, 64, 32)))
        assertEquals("列表为空时原样返回请求值", 80, StoryFeedQuery.pickQuality(80, intArrayOf()))
        assertEquals("列表为 null 时原样返回请求值", 80, StoryFeedQuery.pickQuality(80, null))
    }

    @Test
    fun pickQuality_ignoresNonPositiveAndFallsBackToLowest() {
        assertEquals(
            "非法值应忽略",
            16,
            StoryFeedQuery.pickQuality(64, intArrayOf(0, -1, 16))
        )
        assertEquals(
            "全部合法值都高于请求值时取最低档，避免无流",
            32,
            StoryFeedQuery.pickQuality(16, intArrayOf(80, 32))
        )
    }

    // ---------- 相关推荐（relate/story） ----------

    @Test
    fun buildRelatedParams_carriesAnchorAidAndLoginState() {
        val params = StoryFeedQuery.buildRelatedParams(123456L, "ak", "Pixel 5")

        assertEquals("锚点视频 aid 必须带上", "123456", valueOf(params, "aid"))
        assertEquals("登录态要带上", "ak", valueOf(params, "access_key"))
        assertEquals("设备名要带上", "Pixel 5", valueOf(params, "device_name"))
        assertEquals("复用主信息流公共参数 mobi_app", "android", valueOf(params, "mobi_app"))
        assertEquals("默认 request_from", "0", valueOf(params, "request_from"))
    }

    @Test
    fun parseRelated_supportsItemsObjectShape() {
        val json = JSONObject(
            """{"code":0,"data":{"items":[${verticalItem(1)},${verticalItem(2)}]}}"""
        )

        val items = StoryFeedQuery.parseRelated(json)

        assertEquals("应解析 data.items", listOf(1L, 2L), items.map { it.aid })
    }

    @Test
    fun parseRelated_supportsArrayDataShape() {
        val json = JSONObject("""{"code":0,"data":[${verticalItem(7)},${verticalItem(8)}]}""")

        val items = StoryFeedQuery.parseRelated(json)

        assertEquals("应兼容 data 直接是数组的形态", listOf(7L, 8L), items.map { it.aid })
    }

    @Test
    fun parseRelated_codeNotZeroOrNull_returnsEmpty() {
        assertTrue(
            "接口报错返回空",
            StoryFeedQuery.parseRelated(JSONObject("""{"code":-400,"message":"x"}""")).isEmpty()
        )
        assertTrue("root 为 null 返回空", StoryFeedQuery.parseRelated(null).isEmpty())
    }

    @Test
    fun parseRelated_dropsHorizontalAndSeenAids() {
        val horizontal = verticalItem(3, extra = ""","dimension":{"width":1920,"height":1080,"rotate":0}""")
        val json = JSONObject(
            """{"code":0,"data":{"items":[${verticalItem(1)},$horizontal,${verticalItem(2)}]}}"""
        )

        val items = StoryFeedQuery.parseRelated(json, alreadySeenAids = setOf(2L))

        assertEquals("横屏丢弃 + 已看过的 aid 过滤", listOf(1L), items.map { it.aid })
    }

    // ---------- request_from：场景来源/池选择器 ----------

    @Test
    fun buildParams_sendsDefaultRequestFrom() {
        val params = StoryFeedQuery.buildParams(1, true, null, "", "")

        assertEquals(
            "不显式指定时应带官方默认值 0（此前完全没发这个参数）",
            StoryFeedQuery.DEFAULT_REQUEST_FROM.toString(),
            valueOf(params, "request_from")
        )
    }

    @Test
    fun buildParams_requestFromOutOfRange_clampedToPoolRange() {
        assertEquals(
            "负值应收敛到 0",
            "0",
            valueOf(StoryFeedQuery.buildParams(1, true, null, "", "", requestFrom = -3), "request_from")
        )
        assertEquals(
            "超出 6 应收敛到 6",
            "6",
            valueOf(StoryFeedQuery.buildParams(1, true, null, "", "", requestFrom = 99), "request_from")
        )
    }

    // ---------- 辅助 ----------

    private fun valueOf(params: List<Pair<String, String>>, name: String): String? =
        params.firstOrNull { it.first == name }?.second

    private fun feedOf(vararg items: String): JSONObject =
        JSONObject("""{"code":0,"data":{"items":[${items.joinToString(",")}]}}""")

    private fun verticalItem(aid: Long, cid: Long = aid * 10, extra: String = ""): String =
        """{"goto":"vertical_av","player_args":{"aid":$aid,"cid":$cid},"title":"视频$aid"$extra}"""
}
