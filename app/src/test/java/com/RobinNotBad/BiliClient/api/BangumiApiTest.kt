package com.RobinNotBad.BiliClient.api

import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 追番列表解析测试（26.10.04 批次 5 的 C16）。
 *
 * 解析结果直接决定"要不要弹追番更新通知"：字段取错会让更新永远检测不到（静默失效），
 * 或者把没开播的番当成更新（每次打开应用都弹）。只测纯解析，不发网络请求。
 */
class BangumiApiTest {

    private fun item(
        mediaId: Long,
        title: String = "某番",
        newEpId: Long = 0,
        indexShow: String = ""
    ): String = buildString {
        append("""{"media_id":$mediaId,"title":"$title"""")
        if (newEpId > 0 || indexShow.isNotEmpty()) {
            append(""","new_ep":{"id":$newEpId,"index_show":"$indexShow"}""")
        }
        append("}")
    }

    private fun rootWith(vararg items: String): JSONObject =
        JSONObject("""{"code":0,"data":{"list":[${items.joinToString(",")}],"pn":1,"ps":30,"total":${items.size}}}""")

    @Test
    fun parseFollowingList_取出mediaId与最新集() {
        val list = BangumiApi.parseFollowingList(
            rootWith(
                item(12345L, "葬送的芙莉莲", 307774L, "更新至第13话"),
                item(67890L, "孤独摇滚", 290001L, "全12话")
            )
        )

        assertEquals(2, list.size)
        assertEquals(12345L, list[0].mediaId)
        assertEquals("葬送的芙莉莲", list[0].title)
        assertEquals(307774L, list[0].newEpId)
        assertEquals("更新至第13话", list[0].newEpIndexShow)
        assertEquals(67890L, list[1].mediaId)
        assertEquals(290001L, list[1].newEpId)
    }

    @Test
    fun parseFollowingList_没有new_ep时最新集为0() {
        val list = BangumiApi.parseFollowingList(rootWith(item(111L, "还没开播的番")))

        assertEquals(1, list.size)
        assertEquals(0L, list[0].newEpId)
        assertEquals("", list[0].newEpIndexShow)
    }

    @Test
    fun parseFollowingList_跳过没有media_id的项() {
        val list = BangumiApi.parseFollowingList(
            rootWith(item(0L, "脏数据"), item(222L, "正常番", 5L, "更新至第5话"))
        )

        assertEquals(1, list.size)
        assertEquals(222L, list[0].mediaId)
    }

    @Test
    fun parseFollowingList_缺list或data_返回空表() {
        assertEquals(0, BangumiApi.parseFollowingList(JSONObject("""{"code":0}""")).size)
        assertEquals(0, BangumiApi.parseFollowingList(JSONObject("""{"code":0,"data":{}}""")).size)
        assertEquals(
            0,
            BangumiApi.parseFollowingList(JSONObject("""{"code":0,"data":{"list":null}}""")).size
        )
    }

    @Test
    fun parseFollowingList_错误码_抛出可读异常() {
        var thrown: JSONException? = null
        try {
            BangumiApi.parseFollowingList(JSONObject("""{"code":53013,"message":"用户隐私设置未公开"}"""))
        } catch (e: JSONException) {
            thrown = e
        }

        assertTrue("错误码必须抛出 JSONException", thrown != null)
        assertEquals("用户隐私设置未公开", thrown!!.message)
    }

    @Test
    fun parseFollowingList_错误码但没有message_用错误码兜底() {
        var thrown: JSONException? = null
        try {
            BangumiApi.parseFollowingList(JSONObject("""{"code":-400}"""))
        } catch (e: JSONException) {
            thrown = e
        }

        assertTrue("错误码必须抛出 JSONException", thrown != null)
        assertEquals("错误码：-400", thrown!!.message)
    }

    // ---------- 追番状态（详情页追番按钮） ----------

    @Test
    fun parseFollowStatus_follow为1_已追番() {
        assertTrue(BangumiApi.parseFollowStatus(JSONObject("""{"code":0,"result":{"follow":1}}""")))
    }

    @Test
    fun parseFollowStatus_follow为0_未追番() {
        assertTrue(!BangumiApi.parseFollowStatus(JSONObject("""{"code":0,"result":{"follow":0}}""")))
    }

    @Test
    fun parseFollowStatus_缺result_视为未追番() {
        assertTrue(!BangumiApi.parseFollowStatus(JSONObject("""{"code":0}""")))
        assertTrue(!BangumiApi.parseFollowStatus(JSONObject("""{"code":0,"result":null}""")))
    }

    @Test
    fun parseFollowStatus_错误码_视为未追番() {
        // -101 未登录：显示"追番"比误显示"已追番"安全（点一下会被告知未登录）
        assertTrue(!BangumiApi.parseFollowStatus(JSONObject("""{"code":-101,"message":"账号未登录"}""")))
    }

    // ---------- 季详情解析（详情页页内切季） ----------

    private fun episodeJson(id: Long, title: String = "第1话", longTitle: String = "启程"): String =
        """{"id":$id,"aid":${id + 100},"cid":${id + 200},"cover":"http://x/$id.jpg","badge":"","title":"$title","long_title":"$longTitle"}"""

    @Test
    fun parseSeasonDetail_正片补齐为第一个分区_附加分区排其后() {
        val meta = BangumiApi.parseSeasonDetail(
            JSONObject(
                """{"type":1,"new_ep":{"desc":"已完结，全2话"},""" +
                        """"episodes":[${episodeJson(1)},${episodeJson(2)}],""" +
                        """"section":[{"id":9,"title":"预告花絮","type":1,"episodes":[${episodeJson(3, "PV", "正式预告")}]}]}"""
            )
        )

        assertEquals(1, meta.seasonType)
        assertEquals("已完结，全2话", meta.statusDesc)
        assertEquals(2, meta.sectionList.size)
        // 正片（episodes 平铺数组）必须补成第一个分区，否则切季后的默认选中集无从谈起
        assertEquals("正片", meta.sectionList[0].title)
        assertEquals(2, meta.sectionList[0].episodeList.size)
        assertEquals(1L, meta.sectionList[0].episodeList[0].id)
        assertEquals("预告花絮", meta.sectionList[1].title)
        assertEquals("正式预告", meta.sectionList[1].episodeList[0].title_long)
    }

    @Test
    fun parseSeasonDetail_没有正片集_不补空的正片分区() {
        // 空分区会让切季判断"该季暂无内容"失效，所以正片为空时宁可不补
        val meta = BangumiApi.parseSeasonDetail(
            JSONObject("""{"type":4,"section":[{"id":9,"title":"预告花絮","type":1,"episodes":[${episodeJson(3)}]}]}""")
        )

        assertEquals(1, meta.sectionList.size)
        assertEquals("预告花絮", meta.sectionList[0].title)
        assertEquals(4, meta.seasonType)
        assertEquals("", meta.statusDesc)
    }

    @Test
    fun parseSeasonDetail_空数据_得到空季数据而不是崩溃() {
        val meta = BangumiApi.parseSeasonDetail(JSONObject("{}"))

        assertEquals(0, meta.sectionList.size)
        assertEquals(0, meta.seasonType)
        assertEquals("", meta.statusDesc)
    }
}
