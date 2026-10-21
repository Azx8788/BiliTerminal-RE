package com.RobinNotBad.BiliClient.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BetaUpdateTest {

    @Test
    fun `拼接分享链接`() {
        assertEquals(
            "https://share.lanol.cn/#/?code=12345",
            BetaUpdate.buildShareUrl("12345")
        )
        assertEquals(
            "https://file.zsapp.asia/#/?code=888",
            BetaUpdate.buildShareUrl("888", "https://file.zsapp.asia")
        )
    }

    @Test
    fun `内测码只接受纯数字`() {
        assertEquals("12345", BetaUpdate.normalizeCode("12345"))
        assertEquals("12345", BetaUpdate.normalizeCode(" 12345 "))
        assertNull(BetaUpdate.normalizeCode("12a45"))
        assertNull(BetaUpdate.normalizeCode(""))
        assertNull(BetaUpdate.normalizeCode("   "))
        assertNull(BetaUpdate.normalizeCode("123456789012345678901"))
    }

    @Test
    fun `解析成功-相对下载地址补全`() {
        val json = """
            {"code":200,"message":"ok","detail":{
                "code":"12345","name":"app-standard-arm64.apk","size":10485760,
                "download_url":"/share/download?key=abc&code=12345"}}
        """.trimIndent()

        val result = BetaUpdate.parseSelectResponse(json, "https://share.lanol.cn")
        assertTrue(result is BetaUpdate.Result.Found)
        val found = result as BetaUpdate.Result.Found
        assertEquals("https://share.lanol.cn", found.host)
        assertEquals("app-standard-arm64.apk", found.file.name)
        assertEquals(10485760L, found.file.size)
        assertEquals(
            "https://share.lanol.cn/share/download?key=abc&code=12345",
            found.file.downloadUrl
        )
    }

    @Test
    fun `解析成功-外部绝对地址保持原样`() {
        val json = """
            {"code":200,"detail":{"name":"x.apk","size":1,"download_url":"https://s3.example.com/x.apk"}}
        """.trimIndent()

        val result = BetaUpdate.parseSelectResponse(json, "https://file.zsapp.asia")
        val found = result as BetaUpdate.Result.Found
        assertEquals("https://s3.example.com/x.apk", found.file.downloadUrl)
    }

    @Test
    fun `解析失败-文件不存在`() {
        val json = """{"code":404,"message":"ok","detail":"文件不存在"}"""
        val result = BetaUpdate.parseSelectResponse(json, "https://share.lanol.cn")
        assertTrue(result is BetaUpdate.Result.NotFound)
        assertEquals("文件不存在", (result as BetaUpdate.Result.NotFound).message)
    }

    @Test
    fun `解析失败-文本分享没有下载地址`() {
        val json = """
            {"code":200,"detail":{"name":"note","size":0,"download_url":null,"text":"hello"}}
        """.trimIndent()
        val result = BetaUpdate.parseSelectResponse(json, "https://share.lanol.cn")
        assertTrue(result is BetaUpdate.Result.NotFound)
    }

    @Test
    fun `文件名是否像安装包`() {
        assertTrue(BetaUpdate.isApkFileName("app.apk"))
        assertTrue(BetaUpdate.isApkFileName("APP.APK"))
        assertTrue(BetaUpdate.isApkFileName("  app-release.apk "))
        assertFalse(BetaUpdate.isApkFileName("photo.jpg"))
        assertFalse(BetaUpdate.isApkFileName(""))
        assertFalse(BetaUpdate.isApkFileName("app.apk.zip"))
    }

    @Test
    fun `解码 txt 正文-去掉 BOM`() {
        val withBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "内测公告".toByteArray()
        assertEquals("内测公告", BetaUpdate.decodeText(withBom))
        assertEquals("纯文本", BetaUpdate.decodeText("纯文本".toByteArray()))
        assertEquals("", BetaUpdate.decodeText(ByteArray(0)))
    }

    @Test
    fun `文件名是否像文本`() {
        assertTrue(BetaUpdate.isTextFileName("公告.txt"))
        assertTrue(BetaUpdate.isTextFileName("NOTICE.TXT"))
        assertFalse(BetaUpdate.isTextFileName("app.apk"))
        assertFalse(BetaUpdate.isTextFileName(""))
        assertFalse(BetaUpdate.isTextFileName("readme.txt.exe"))
    }

    @Test
    fun `解析成功-txt 文本按可下载文件返回`() {
        val json = """
            {"code":200,"detail":{"name":"内测公告.txt","size":42,
            "download_url":"/share/download?key=k&code=12345"}}
        """.trimIndent()
        val result = BetaUpdate.parseSelectResponse(json, "https://share.lanol.cn")
        assertTrue(result is BetaUpdate.Result.Found)
        val found = result as BetaUpdate.Result.Found
        assertEquals("内测公告.txt", found.file.name)
        assertTrue(BetaUpdate.isTextFileName(found.file.name))
        assertEquals("https://share.lanol.cn/share/download?key=k&code=12345", found.file.downloadUrl)
    }

    @Test
    fun `解析失败-内测码对应的是图片不是安装包`() {
        val json = """
            {"code":200,"detail":{"name":"17924685876121194154933954809420.jpg","size":1000,
            "download_url":"/share/download?key=k&code=12345"}}
        """.trimIndent()
        val result = BetaUpdate.parseSelectResponse(json, "https://share.lanol.cn")
        assertTrue(result is BetaUpdate.Result.NotFound)
        assertEquals("该内测码对应的文件不是安装包", (result as BetaUpdate.Result.NotFound).message)
    }

    @Test
    fun `解析失败-文本分享标记 is_text`() {
        val json = """
            {"code":200,"detail":{"name":"app.apk","size":10,
            "download_url":"/share/download?key=k","is_text":true}}
        """.trimIndent()
        assertTrue(BetaUpdate.parseSelectResponse(json, "https://share.lanol.cn") is BetaUpdate.Result.NotFound)
    }

    @Test
    fun `坏 JSON`() {
        val result = BetaUpdate.parseSelectResponse("<html>502</html>", "https://share.lanol.cn")
        assertTrue(result is BetaUpdate.Result.NotFound)
    }

    @Test
    fun `请求体是 JSON`() {
        assertEquals("{\"code\":\"12345\"}", BetaUpdate.buildSelectBody("12345"))
    }

    @Test
    fun `大小格式化`() {
        assertEquals("未知大小", BetaUpdate.formatSize(0))
        assertEquals("512 B", BetaUpdate.formatSize(512))
        assertEquals("1.5 KB", BetaUpdate.formatSize(1536))
        assertEquals("10.0 MB", BetaUpdate.formatSize(10 * 1024 * 1024))
    }
}
