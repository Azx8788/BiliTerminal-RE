package com.RobinNotBad.BiliClient.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [LinkUrlUtil.parseTopicId] / [LinkUrlUtil.parseTopicSearchKeyword] 的纯函数单测
 * （26.10.05 话题可跳转的修复；26.10.19 发现真实 jump_url 是搜索链接，补搜索分支）。
 *
 * 话题节点只带一个 `jump_url`：能解析出 `topic_id` 就进站内话题页，
 * 只能解析出搜索关键词就进站内搜索页（实测 `feed/all`/`detail` 下发的都是搜索链接），
 * 两者都解析不出才退化成只上色不可点。"话题能不能点得动"全靠这两个函数，钉在测试里。
 */
class LinkUrlUtilTest {

    @Test
    fun parseTopicId_readsIdFromRealJumpUrl() {
        assertEquals(
            1305890L,
            LinkUrlUtil.parseTopicId("https://m.bilibili.com/topic-detail?topic_id=1305890")
        )
    }

    @Test
    fun parseTopicId_ignoresTopicNameAfterId() {
        // 真实抓包里 topic_name 是 URL 编码的中文，且排在 topic_id 之后
        assertEquals(
            1314000L,
            LinkUrlUtil.parseTopicId(
                "https://m.bilibili.com/topic-detail?topic_id=1314000&topic_name=%E6%B4%9B%E5%A4%A9"
            )
        )
    }

    @Test
    fun parseTopicId_handlesIdNotBeingFirstQueryParam() {
        assertEquals(
            1061343L,
            LinkUrlUtil.parseTopicId(
                "https://m.bilibili.com/topic-detail?from=dynamic&topic_id=1061343&topic_name=x"
            )
        )
    }

    @Test
    fun parseTopicId_returnsZeroWhenAbsentOrUnparsable() {
        // 都返回 0（而不是抛异常），调用方据此走"只上色不可点"的退化分支
        assertEquals(0L, LinkUrlUtil.parseTopicId(null))
        assertEquals(0L, LinkUrlUtil.parseTopicId(""))
        assertEquals(0L, LinkUrlUtil.parseTopicId("https://m.bilibili.com/topic-detail"))
        assertEquals(0L, LinkUrlUtil.parseTopicId("https://m.bilibili.com/topic-detail?topic_name=x"))
        assertEquals(0L, LinkUrlUtil.parseTopicId("#A#"))
    }

    @Test
    fun parseTopicId_doesNotMatchTopicNameContainingId() {
        // 防"匹配到 topic_name 里的 topic_id=" 这类串味；锚定在 ? 或 & 之后
        assertEquals(
            123L,
            LinkUrlUtil.parseTopicId("https://m.bilibili.com/topic-detail?topic_name=a&topic_id=123")
        )
    }

    // ---------------------------------------------------------------- 搜索链接（26.10.19 实测的真实形态）
    // 真实接口里 RICH_TEXT_NODE_TYPE_TOPIC 的 jump_url 就是这个样子，没有 topic_id，
    // 所以"话题能不能点"实际靠的是从它里面取关键词去站内搜索。

    @Test
    fun parseTopicSearchKeyword_decodesProtocolRelativeRealJumpUrl() {
        // 线上抓包原文：#星塔旅人# → //search.bilibili.com/all?keyword=%E6%98%9F%E5%A1%94%E6%97%85%E4%BA%BA
        assertEquals(
            "星塔旅人",
            LinkUrlUtil.parseTopicSearchKeyword(
                "//search.bilibili.com/all?keyword=%E6%98%9F%E5%A1%94%E6%97%85%E4%BA%BA"
            )
        )
    }

    @Test
    fun parseTopicSearchKeyword_acceptsHttpsAndExtraParams() {
        // B 站有时会带上 from_source 之类的参数，keyword 不在最后也要能取到
        assertEquals(
            "绝区零",
            LinkUrlUtil.parseTopicSearchKeyword(
                "https://search.bilibili.com/all?keyword=%E7%BB%9D%E5%8C%BA%E9%9B%B6&from_source=dynamic"
            )
        )
    }

    @Test
    fun parseTopicSearchKeyword_returnsEmptyForNonSearchLinks() {
        // 非 search 域名一律不认（避免把任意带 keyword 的链接当话题搜索）
        assertEquals("", LinkUrlUtil.parseTopicSearchKeyword("https://m.bilibili.com/topic-detail?keyword=x"))
        assertEquals("", LinkUrlUtil.parseTopicSearchKeyword("https://www.bilibili.com/video/BV1xx?keyword=x"))
        // 有域名但没有 keyword
        assertEquals("", LinkUrlUtil.parseTopicSearchKeyword("//search.bilibili.com/all?from_source=dynamic"))
        // 空关键词、空串、null
        assertEquals("", LinkUrlUtil.parseTopicSearchKeyword("//search.bilibili.com/all?keyword="))
        assertEquals("", LinkUrlUtil.parseTopicSearchKeyword(""))
        assertEquals("", LinkUrlUtil.parseTopicSearchKeyword(null))
    }

    @Test
    fun parseTopicSearchKeyword_doesNotMistakeTopicDetailForSearch() {
        // topic_id 链接不含 search 域名 ⇒ 空串，调用方仍走"进话题页"分支
        assertEquals(
            "",
            LinkUrlUtil.parseTopicSearchKeyword("https://m.bilibili.com/topic-detail?topic_id=1305890")
        )
    }
}
