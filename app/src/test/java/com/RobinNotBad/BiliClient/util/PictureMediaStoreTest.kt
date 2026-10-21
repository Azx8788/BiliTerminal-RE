package com.RobinNotBad.BiliClient.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 锁住 [PictureMediaStore] 里两段「不看 Android 运行时」的逻辑。
 *
 * 背景（26.10.21「图片下载器下载的图片找不到」）：图片下载写完从不登记媒体库，
 * 相册/图库（只查 MediaStore）里永远没有刚下好的图。修复靠 [PictureMediaStore.scan]，
 * 而它依赖的两块判定 —— 该不该登记（MIME 认得出来吗）、提示里写什么路径 ——
 * 抽成纯函数后在这里钉住，避免以后「扫描失败/文案回退」悄悄复发。
 */
class PictureMediaStoreTest {

    @Test
    fun 常见图片后缀都能认出MIME() {
        assertEquals("image/jpeg", PictureMediaStore.mimeTypeOf("noface.jpg"))
        assertEquals("image/jpeg", PictureMediaStore.mimeTypeOf("cover.jpeg"))
        assertEquals("image/png", PictureMediaStore.mimeTypeOf("a.png"))
        assertEquals("image/webp", PictureMediaStore.mimeTypeOf("b.webp"))
        assertEquals("image/gif", PictureMediaStore.mimeTypeOf("c.gif"))
        assertEquals("image/bmp", PictureMediaStore.mimeTypeOf("d.bmp"))
        assertEquals("image/heic", PictureMediaStore.mimeTypeOf("e.heic"))
        assertEquals("image/avif", PictureMediaStore.mimeTypeOf("f.avif"))
    }

    @Test
    fun 大写后缀也认() {
        // B 站图床的 URL 大小写都有，别因为 IMG.JPG 就漏登记
        assertEquals("image/jpeg", PictureMediaStore.mimeTypeOf("IMG.JPG"))
        assertEquals("image/png", PictureMediaStore.mimeTypeOf("PIC.PNG"))
    }

    @Test
    fun 非图片与无后缀返回null() {
        // 返回 null 表示「别登记」：下载失败时落盘的是错误页/JSON，塞进相册只会更糟
        assertNull(PictureMediaStore.mimeTypeOf("video.mp4"))
        assertNull(PictureMediaStore.mimeTypeOf("page.html"))
        assertNull(PictureMediaStore.mimeTypeOf("noface"))
        assertNull(PictureMediaStore.mimeTypeOf(""))
    }

    @Test
    fun 提示路径裁成相对外部存储的层级() {
        val root = "/storage/emulated/0"
        assertEquals(
            "Pictures/哔哩终端",
            PictureMediaStore.displayFolder("$root/Pictures/哔哩终端", root)
        )
    }

    @Test
    fun 根路径带不带结尾斜杠结果一致() {
        assertEquals(
            "Pictures/哔哩终端",
            PictureMediaStore.displayFolder("/storage/emulated/0/Pictures/哔哩终端", "/storage/emulated/0/")
        )
    }

    @Test
    fun 不在外部存储下的自定义路径原样返回() {
        // 用户把保存路径改到别处时，裁不出相对路径就老老实实显示绝对路径，
        // 总比显示一个残缺路径好
        assertEquals(
            "/data/local/tmp/pic",
            PictureMediaStore.displayFolder("/data/local/tmp/pic", "/storage/emulated/0")
        )
    }

    @Test
    fun 外部存储根为空时不裁() {
        assertEquals(
            "/storage/emulated/0/Pictures/哔哩终端",
            PictureMediaStore.displayFolder("/storage/emulated/0/Pictures/哔哩终端", "")
        )
    }
}
