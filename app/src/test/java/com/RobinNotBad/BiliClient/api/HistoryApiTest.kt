package com.RobinNotBad.BiliClient.api

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 观看记录兜底查询的纯解析测试（26.10.11 批次 6 的进度线）。
 *
 * 这道闸门决定"番剧续播用哪一集的进度"：宽松了会把别的集的进度套到这一集上
 * （表现是"点开这一集却从上一集的位置开始播"），严格错了又会让续播永远从 0 开始。
 * 只测纯解析，不发网络请求。
 */
class HistoryApiTest {

    /** 造一条观看记录：history.oid 是剧集 avid，history.epid 是剧集身份，progress 单位秒。 */
    private fun record(aid: Long, epid: Long, progress: Int): JSONObject = JSONObject(
        """{"history":{"oid":$aid,"epid":$epid},"progress":$progress}"""
    )

    private fun list(vararg items: JSONObject): JSONArray = JSONArray().apply {
        items.forEach { put(it) }
    }

    @Test
    fun parseHistoryProgressMs_命中同一集_返回毫秒进度() {
        val ms = HistoryApi.parseHistoryProgressMs(list(record(1001L, 500L, 123), record(1002L, 501L, 45)), 1002L, 501L)
        assertEquals(45000L, ms)
    }

    @Test
    fun parseHistoryProgressMs_oid命中但epid不符_丢弃不串集() {
        //同一 aid 下的记录 epid 是别的集：必须丢弃，否则会把上一集的进度套过来
        val ms = HistoryApi.parseHistoryProgressMs(list(record(1001L, 999L, 300)), 1001L, 500L)
        assertEquals(0L, ms)
    }

    @Test
    fun parseHistoryProgressMs_epid传0_退化为只按aid匹配() {
        val ms = HistoryApi.parseHistoryProgressMs(list(record(1001L, 999L, 30)), 1001L, 0L)
        assertEquals(30000L, ms)
    }

    @Test
    fun parseHistoryProgressMs_oid不符_丢弃() {
        val ms = HistoryApi.parseHistoryProgressMs(list(record(1001L, 500L, 300)), 2002L, 500L)
        assertEquals(0L, ms)
    }

    @Test
    fun parseHistoryProgressMs_progress为0或已看完的负1_都不当作可续播位置() {
        assertEquals(0L, HistoryApi.parseHistoryProgressMs(list(record(1001L, 500L, 0)), 1001L, 500L))
        assertEquals(0L, HistoryApi.parseHistoryProgressMs(list(record(1001L, 500L, -1)), 1001L, 500L))
    }

    @Test
    fun parseHistoryProgressMs_缺history节点的脏数据_跳过而不是崩溃() {
        val dirty = JSONArray().apply {
            put(JSONObject("""{"progress":300}"""))
            put(record(1001L, 500L, 42))
        }
        assertEquals(42000L, HistoryApi.parseHistoryProgressMs(dirty, 1001L, 500L))
    }

    @Test
    fun parseHistoryProgressMs_列表为null_返回0() {
        assertEquals(0L, HistoryApi.parseHistoryProgressMs(null, 1001L, 500L))
    }

    @Test
    fun parseHistoryProgressMs_列表为空_返回0() {
        assertEquals(0L, HistoryApi.parseHistoryProgressMs(JSONArray(), 1001L, 500L))
    }

    @Test
    fun progressFinished_协议值就是负1() {
        //这个值是服务端约定的"已看完"，改了会让"看完一集"被当成"看到 -1 秒"
        assertEquals(-1L, HistoryApi.PROGRESS_FINISHED)
    }
}
