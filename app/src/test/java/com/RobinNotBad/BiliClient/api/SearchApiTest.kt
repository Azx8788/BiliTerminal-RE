package com.RobinNotBad.BiliClient.api

import com.RobinNotBad.BiliClient.model.VideoCard
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 搜索结果解析的纯函数单测。
 *
 * 26.10.19 之前聚合搜索（all/v2）的 media_bangumi 分支与单独搜索（search_type=media_bangumi）
 * 是两套解析：前者用 getString 取字段（缺 index_show 整页 JSONException）、封面也拿不到 scheme。
 * 现在两处共用 [SearchApi.getBangumiFromSearchResult]，这里同时钉住单独搜索与聚合搜索两条入口。
 */
class SearchApiTest {

    private fun single(input: JSONArray): List<VideoCard> {
        val list = ArrayList<VideoCard>()
        SearchApi.getBangumiFromSearchResult(input, list)
        return list
    }

    private fun aggregate(input: JSONArray): List<VideoCard> {
        val list = ArrayList<VideoCard>()
        SearchApi.getVideosFromSearchResult(input, list, true)
        return list
    }

    @Test
    fun bangumi_parsesEveryField() {
        val cards = single(
            JSONArray(
                """[{"title":"<em class=\"keyword\">孤独</em>摇滚!","cover":"//i0.hdslb.com/bfs/x.jpg",
                    "areas":"日本","index_show":"全14话","media_id":123,"season_id":"456"}]"""
            )
        )

        assertEquals(1, cards.size)
        val card = cards[0]
        assertEquals("孤独摇滚!", card.title)
        assertEquals("日本", card.upName)
        assertEquals("全14话", card.view)
        assertEquals("https://i0.hdslb.com/bfs/x.jpg", card.cover)
        assertEquals(123L, card.aid)
        assertEquals("456", card.bvid)
        assertEquals("media_bangumi", card.type)
    }

    @Test
    fun bangumi_missingIndexShowFallsBackToStyles() {
        val cards = single(
            JSONArray("""[{"title":"某番","cover":"//i0.hdslb.com/a.jpg","areas":"日本","styles":"原创/科幻","media_id":1,"season_id":"2"}]""")
        )

        // 单独搜索接口没有 index_show，空着会让卡片第三行整行消失，故退用 styles
        assertEquals("原创/科幻", cards[0].view)
    }

    @Test
    fun bangumi_toleratesMissingFields() {
        // 旧实现用 getString 取 index_show/cover，缺字段直接抛 JSONException 整页失败
        val cards = single(JSONArray("""[{"title":"只有标题"}]"""))

        assertEquals(1, cards.size)
        assertEquals("", cards[0].cover)
        assertEquals("", cards[0].view)
        assertEquals(0L, cards[0].aid)
        assertEquals("", cards[0].bvid)
    }

    @Test
    fun bangumi_coverWithoutSchemeGetsHttps() {
        val cards = single(
            JSONArray("""[{"title":"x","cover":"//i0.hdslb.com/b.jpg"},{"title":"y","cover":"https://i0.hdslb.com/c.jpg"}]""")
        )
        assertEquals("https://i0.hdslb.com/b.jpg", cards[0].cover)
        assertEquals("https://i0.hdslb.com/c.jpg", cards[1].cover)
    }

    @Test
    fun bangumi_skipsNullEntriesAndToleratesNullInput() {
        val cards = single(JSONArray("""[null,{"title":"有效"}]"""))
        assertEquals(1, cards.size)
        assertEquals("有效", cards[0].title)

        val nullList = ArrayList<VideoCard>()
        // 防御式接口：null 不该抛
        SearchApi.getBangumiFromSearchResult(null, nullList)
        assertTrue(nullList.isEmpty())
    }

    @Test
    fun aggregateSearch_bangumiBranchReusesTheSameParser() {
        val cards = aggregate(
            JSONArray(
                """[{"result_type":"media_bangumi","data":[
                    {"title":"<em class=\"keyword\">间谍</em>过家家","cover":"//i0.hdslb.com/d.jpg","areas":"日本",
                     "styles":"搞笑/日常","media_id":999,"season_id":"888"}]}]"""
            )
        )

        assertEquals(1, cards.size)
        val card = cards[0]
        assertEquals("间谍过家家", card.title)
        assertEquals("https://i0.hdslb.com/d.jpg", card.cover)
        assertEquals("搞笑/日常", card.view)
        assertEquals(999L, card.aid)
        assertEquals("media_bangumi", card.type)
    }
}
