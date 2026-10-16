package com.RobinNotBad.BiliClient.api

import com.RobinNotBad.BiliClient.util.FakeSharedPreferences
import com.RobinNotBad.BiliClient.util.SettingsKeys
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 「我的反馈」（只读）的解析与 X-Mid 策略（26.10.16）。
 *
 * 这一段全是**静默错误**：写错了 App 照样编译、能装、界面也对，坏掉的只是发到自建服务器上的
 * 请求与页面上那几行字——例如状态写反（把「待受理」当成「已受理」）、回复丢了、
 * 或者在用户没勾选「附带账号 ID」时把 UID 送了出去。所以逐条钉住：
 *
 * 1. `parseMyFeedback` 的字段映射（含 `handled` 语义与 `ctime` 秒→毫秒）；
 * 2. 回复列表的三种形态（多条 / 空数组 / 字段缺失）都不炸；
 * 3. **`X-Mid` 只在「勾选了开关 且 已登录」时才带**，其余一律不带。
 *
 * 第 3 条由 `shouldAttachMid()` 单独承担——`feedbackHeaders()` 本身会经 `getInstallId()`
 * 调到 `android.text.TextUtils`，那在 JVM 单测里是 not mocked、跑不了；把「要不要带」
 * 这一步抽出来只读 SharedPreferences，就能在这里直接验证。SP 用共享的 [FakeSharedPreferences]。
 */
class TerminalMyFeedbackTest {

    @Before
    fun setUp() {
        SharedPreferencesUtil.sharedPreferences = FakeSharedPreferences()
    }

    @After
    fun tearDown() {
        SharedPreferencesUtil.sharedPreferences = null
    }

    // ---------- parseMyFeedback ----------

    @Test
    fun parseMyFeedback_parsesItemAndReplies() {
        val data = JSONObject(
            """
            {"items":[
              {"id":3,"category":"bug","content":"播放器切集闪退","contact":"qq:123","ctime":1791727969,
               "handled":1,"replies":[
                 {"id":1000000003,"title":"已修复","content":"下个版本会修好","ctime":1791728000}
               ]}
            ],"total":1}
            """.trimIndent()
        )

        val list = TerminalApi.parseMyFeedback(data)

        assertEquals(1, list.size)
        val item = list[0]
        assertEquals(3, item.id)
        assertEquals("bug", item.category)
        assertEquals("播放器切集闪退", item.content)
        assertEquals("qq:123", item.contact)
        assertTrue("handled=1 应解析成「已受理」", item.handled)
        assertTrue(
            "ctime 秒应被换算成可读时间",
            item.ctime.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"))
        )

        assertEquals(1, item.replies.size)
        val reply = item.replies[0]
        assertEquals(1000000003, reply.id)
        assertEquals("已修复", reply.title)
        assertEquals("下个版本会修好", reply.content)
        assertTrue(reply.ctime.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")))
    }

    @Test
    fun parseMyFeedback_handledZeroMeansPending() {
        val data = JSONObject(
            """{"items":[{"id":1,"category":"other","content":"x","handled":0,"replies":[]}]}"""
        )
        val item = TerminalApi.parseMyFeedback(data)[0]
        assertFalse("handled=0 必须解析成「待受理」而不是已受理", item.handled)
    }

    @Test
    fun parseMyFeedback_toleratesAllReplyShapes() {
        // 空数组 / 字段缺失 / 多条 —— 三种都要收敛成列表，且条数正确
        val data = JSONObject(
            """
            {"items":[
              {"id":1,"category":"bug","content":"a","handled":0,"replies":[]},
              {"id":2,"category":"bug","content":"b","handled":1},
              {"id":3,"category":"bug","content":"c","handled":1,"replies":[
                 {"id":11,"title":"t1","content":"c1","ctime":100},
                 {"id":12,"title":"t2","content":"c2","ctime":200}
              ]}
            ]}
            """.trimIndent()
        )

        val list = TerminalApi.parseMyFeedback(data)
        assertEquals(3, list.size)
        assertTrue("空 replies 应是空列表", list[0].replies.isEmpty())
        assertTrue("缺 replies 字段应是空列表", list[1].replies.isEmpty())
        assertEquals("多条回复都要保留", 2, list[2].replies.size)
        assertEquals(12, list[2].replies[1].id)
    }

    @Test
    fun parseMyFeedback_nullEmptyOrMissingItemsYieldsEmptyList() {
        assertTrue(TerminalApi.parseMyFeedback(null).isEmpty())
        assertTrue(TerminalApi.parseMyFeedback(JSONObject("{}")).isEmpty())
        assertTrue(TerminalApi.parseMyFeedback(JSONObject("""{"items":[],"total":0}""")).isEmpty())
    }

    @Test
    fun parseMyFeedback_skipsJsonNullEntry() {
        val data = JSONObject("""{"items":[null,{"id":9,"category":"bug","content":"ok"}]}""")
        val list = TerminalApi.parseMyFeedback(data)
        assertEquals("数组里的 null 条目应被跳过而不是崩溃", 1, list.size)
        assertEquals(9, list[0].id)
    }

    // ---------- 对话两侧的 author ----------

    /**
     * 26.10.16 起一条反馈下面是**两侧**的消息（开发者回复 / 用户追加说明），靠 `author` 区分。
     * 判错的话界面照样出得来，只是把「我写的」标成「开发者回复」——只能靠人眼看出来。
     */
    @Test
    fun parseMyFeedback_readsBothAuthorsInOrder() {
        val data = JSONObject(
            """
            {"items":[{"id":7,"category":"bug","content":"闪退","handled":0,"replies":[
              {"id":1000000009,"author":"dev","title":"已修复","content":"下版修好","ctime":100},
              {"id":3,"author":"user","title":"","content":"补充：点下一集才复现","ctime":200}
            ]}]}
            """.trimIndent()
        )

        val replies = TerminalApi.parseMyFeedback(data)[0].replies
        assertEquals("两侧消息都要保留", 2, replies.size)
        assertFalse("作者是 dev 时 isFromUser 必须为 false", replies[0].isFromUser())
        assertTrue("作者是 user 时 isFromUser 必须为 true", replies[1].isFromUser())
        assertEquals("用户消息没有标题", "", replies[1].title)
        assertEquals("服务端给的顺序要原样保留", 3, replies[1].id)
    }

    /**
     * **旧服务端不返回 `author`**（那时只有开发者回复这一侧）。
     * 缺失时必须落成「开发者」，而不是被当成用户消息 —— 否则升级客户端后会把自己没写过的话
     * 标成「我的补充」。
     */
    @Test
    fun parseMyFeedback_missingAuthorMeansDev() {
        val data = JSONObject(
            """{"items":[{"id":1,"category":"bug","content":"x","handled":1,"replies":[
                 {"id":1000000001,"title":"已修复","content":"好了","ctime":100}
               ]}]}"""
        )
        val reply = TerminalApi.parseMyFeedback(data)[0].replies[0]
        assertEquals("", reply.author)
        assertFalse("author 缺失时应按开发者回复处理", reply.isFromUser())
    }

    /** `author` 是 `null` 或未知值时也不能崩，且一律不当成用户消息。 */
    @Test
    fun parseMyFeedback_toleratesWeirdAuthor() {
        val data = JSONObject(
            """{"items":[{"id":1,"category":"bug","content":"x","handled":0,"replies":[
                 {"id":1,"author":null,"title":"t","content":"a","ctime":1},
                 {"id":2,"author":"ADMIN","title":"t","content":"b","ctime":2}
               ]}]}"""
        )
        val replies = TerminalApi.parseMyFeedback(data)[0].replies
        assertEquals(2, replies.size)
        assertFalse(replies[0].isFromUser())
        assertFalse("author 比对是大小写敏感的，未知值不算用户", replies[1].isFromUser())
    }

    // ---------- X-Mid 策略 ----------

    @Test
    fun shouldAttachMid_requiresBothSwitchAndMid() {
        SharedPreferencesUtil.putLong(SharedPreferencesUtil.mid, 123456L)
        SharedPreferencesUtil.putBoolean(SettingsKeys.FEEDBACK_ATTACH_MID, true)
        assertTrue("勾选了开关且已登录，才该带 X-Mid", TerminalApi.shouldAttachMid())
    }

    @Test
    fun shouldAttachMid_offWhenSwitchNotChecked() {
        SharedPreferencesUtil.putLong(SharedPreferencesUtil.mid, 123456L)
        SharedPreferencesUtil.putBoolean(SettingsKeys.FEEDBACK_ATTACH_MID, false)
        assertFalse("没勾选「附带账号 ID」就不能带 X-Mid", TerminalApi.shouldAttachMid())
    }

    @Test
    fun shouldAttachMid_offWhenNotLoggedIn() {
        SharedPreferencesUtil.putLong(SharedPreferencesUtil.mid, 0L)
        SharedPreferencesUtil.putBoolean(SettingsKeys.FEEDBACK_ATTACH_MID, true)
        assertFalse("未登录（无 UID）时没有可带的 mid", TerminalApi.shouldAttachMid())
    }

    @Test
    fun shouldAttachMid_offByDefault() {
        // 全新安装：开关默认 false、也没有登录态，绝不能带
        assertFalse(TerminalApi.shouldAttachMid())
    }
}
