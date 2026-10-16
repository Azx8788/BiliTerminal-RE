package com.RobinNotBad.BiliClient.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 内存日志缓冲的脱敏规则（[LogSanitizer]）。
 *
 * 钉住三件**不会崩、只会静默泄密**的事：
 *
 * 1. **凭证的值绝不能留在日志里**——`access_key` / `csrf` / `SESSDATA` 都是可直接冒用身份的
 *    凭证，而内存日志快照会随崩溃 / 反馈导出给用户，漏一个等于把会话明文送人。
 * 2. **脱敏要覆盖日志行的所有真实形态**——整行 URL、Cookie 串（`;` 分隔）、只有一小段带参数
 *    的片段。按 URL 解析会漏掉后两种，所以这里按 `键=值` 做文本替换。
 * 3. **快速路径必须真的快**——绝大多数日志行一个受管键名都没有，必须原样返回、不跑正则，
 *    否则每行日志都跑一遍正则在手表端不可接受。
 *
 * 本类**不依赖任何 `android.*` 类**，可在纯 JVM 单测里跑。
 */
class LogSanitizerTest {

    // ---------- URL 查询串（`&` 分隔） ----------

    @Test
    fun 查询串里的敏感值被打码() {
        val out = LogSanitizer.sanitize("GET /x?access_key=SECRET&csrf=SECRET_JCT&b=2")
        assertFalse("access_key 的值不能留下", out.contains("SECRET"))
        assertFalse("csrf 的值不能留下", out.contains("SECRET_JCT"))
        assertTrue("键名要保留，便于确认参数存在", out.contains("access_key=***"))
        assertTrue(out.contains("csrf=***"))
        assertTrue("非敏感参数原样保留，agent 靠它判断接口语义", out.contains("b=2"))
    }

    @Test
    fun 值在行尾结束时也能打码() {
        val out = LogSanitizer.sanitize("?appkey=KEY123")
        assertFalse(out.contains("KEY123"))
        assertEquals("?appkey=***", out)
    }

    @Test
    fun 空值的敏感参数同样打码() {
        // `csrf=`（空值）也不能原样留着，与 NetWorkUtil.maskUrl 的约定一致
        val out = LogSanitizer.sanitize("x?csrf=&b=1")
        assertTrue(out.contains("csrf=***"))
        assertTrue(out.contains("b=1"))
    }

    // ---------- Cookie 串（`;` 分隔） ----------

    @Test
    fun cookie串里的敏感值被打码() {
        val out = LogSanitizer.sanitize("Cookie: SESSDATA=abc123; bili_jct=def456; buvid3=keepme")
        assertFalse(out.contains("abc123"))
        assertFalse(out.contains("def456"))
        assertTrue(out.contains("SESSDATA=***"))
        assertTrue(out.contains("bili_jct=***"))
        assertTrue("非敏感 Cookie 原样保留", out.contains("buvid3=keepme"))
    }

    @Test
    fun 值在空白处结束时也能打码() {
        val out = LogSanitizer.sanitize("token=abc def=ghi")
        assertFalse(out.contains("abc"))
        assertTrue(out.contains("token=***"))
        assertTrue("非受管键不受影响", out.contains("def=ghi"))
    }

    // ---------- 大小写不敏感 ----------

    @Test
    fun 键名大小写不敏感() {
        val out = LogSanitizer.sanitize("sessdata=lower&APPKEY=upper&Refresh_Token=mixed")
        assertFalse(out.contains("lower"))
        assertFalse(out.contains("upper"))
        assertFalse(out.contains("mixed"))
        assertTrue(out.contains("sessdata=***"))
        assertTrue(out.contains("APPKEY=***"))
        assertTrue(out.contains("Refresh_Token=***"))
    }

    // ---------- 噪音键 ----------

    @Test
    fun 噪音键的值替换成省略号() {
        val blob = "x".repeat(1500)
        val out = LogSanitizer.sanitize("wbi?dm_img_list=$blob&dm_img_str=ABC&pn=1")
        assertFalse("指纹 blob 不能整段留下", out.contains(blob))
        assertTrue("噪音值用省略号，与 maskUrl 一致", out.contains("dm_img_list=…"))
        assertTrue(out.contains("dm_img_str=…"))
        assertTrue(out.contains("pn=1"))
    }

    // ---------- 快速路径 / 不误伤 ----------

    @Test
    fun 无受管键名时原样返回同一个对象() {
        // 快速路径的判据：不跑正则，直接 return 入参本身
        val text = "PlayerActivity>onPrepared: 视频已就绪，共 123 秒"
        assertSame("没有键名时必须原样返回、不跑正则", text, LogSanitizer.sanitize(text))
    }

    @Test
    fun 词中间出现的键名片段不误伤() {
        // "design" 里含 "sign"，但它前面不是词边界，不该把 design 的值打码
        val out = LogSanitizer.sanitize("design=okay")
        assertEquals("design=okay", out)
    }

    // ---------- null / 空串 ----------

    @Test
    fun null与空串() {
        assertEquals("null 返回空串，与 maskUrl 的 null 语义一致", "", LogSanitizer.sanitize(null))
        assertEquals("", LogSanitizer.sanitize(""))
    }
}
