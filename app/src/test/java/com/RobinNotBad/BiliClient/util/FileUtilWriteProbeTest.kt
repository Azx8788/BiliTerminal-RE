package com.RobinNotBad.BiliClient.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 锁住两件"只碰 java.io.File"的事，都能在纯 JVM 单测里跑（不依赖 Android 运行时）：
 *
 * 1. [FileUtil.canWriteDirectory]：**真的去试写**，而不是看授权状态。
 *    背景（issue #42）：默认下载目录 `<外部存储>/Android/media/<包名>/` 是应用专属外部存储，
 *    Android 10+ 写这里不需要任何权限，但旧实现用 `Environment.isExternalStorageManager()`
 *    一律判成「没权限」，于是每次下载都把用户顶到系统设置页；手表 ROM 没有那个设置页，
 *    用户永远授不到权限，默认路径下的下载被永久挡死。
 * 2. [FileUtil.cleanDownloadTempFiles]：只清本次下载的临时产物，**绝不删已下好的成品**。
 *    这里最要命的是 .bak：它可能是"旧成品改名成 .bak 之后、新文件改名成成品之前"被打断时
 *    唯一幸存的成品，删掉就是永久丢数据。
 */
class FileUtilWriteProbeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun 已存在的可写目录返回true且不留下探测文件() {
        val dir = tempFolder.newFolder("downloads")

        assertTrue(FileUtil.canWriteDirectory(dir))
        // 探测文件必须自己清理干净，否则会在下载目录里留垃圾
        assertTrue((dir.listFiles() ?: emptyArray()).isEmpty())
    }

    @Test
    fun 不存在的深层目录会被创建并返回true() {
        val dir = File(tempFolder.root, "a/b/c")

        assertTrue(FileUtil.canWriteDirectory(dir))
        assertTrue("探测成功应当把目录建出来", dir.isDirectory)
        assertTrue((dir.listFiles() ?: emptyArray()).isEmpty())
    }

    @Test
    fun 目录里已有文件不影响判定() {
        val dir = tempFolder.newFolder("with_files")
        tempFolder.newFile("with_files/video.mp4")

        assertTrue(FileUtil.canWriteDirectory(dir))
        assertTrue(File("$dir/video.mp4").exists())
    }

    @Test
    fun 建不出目录时返回false() {
        // 父节点是个文件 → mkdirs 必然失败，等价于「这里写不进去」
        val blocker = tempFolder.newFile("blocker")
        val dir = File(blocker, "sub")

        assertFalse(FileUtil.canWriteDirectory(dir))
    }

    @Test
    fun null返回false() {
        assertFalse(FileUtil.canWriteDirectory(null))
    }

    // ---------- cleanDownloadTempFiles：只清临时产物，成品一个都不能少 ----------

    @Test
    fun 成品存在时残留的bak被删掉() {
        val dir = tempFolder.newFolder("bak_with_final")
        val video = tempFolder.newFile("bak_with_final/video.mp4")
        val bak = tempFolder.newFile("bak_with_final/video.mp4.bak")

        FileUtil.cleanDownloadTempFiles(dir)

        assertTrue("成品必须保留", video.exists())
        assertFalse("成品还在，.bak 只是替换成功后没删掉的备份", bak.exists())
    }

    @Test
    fun 只有bak时把它改名回成品而不是删掉() {
        // 进程在"旧成品改名成 .bak"之后、"新临时文件改名成成品"之前被回收：.bak 是当时唯一的成品
        val dir = tempFolder.newFolder("bak_only")
        val bak = tempFolder.newFile("bak_only/video.mp4.bak")
        bak.writeText("old video")

        FileUtil.cleanDownloadTempFiles(dir)

        val restored = File(dir, "video.mp4")
        assertTrue("唯一幸存的旧成品必须从 .bak 恢复，不能删", restored.exists())
        assertEquals("恢复出来的内容必须是原来那份", "old video", restored.readText())
        assertFalse(".bak 已改名，不应残留", bak.exists())
    }

    @Test
    fun 下载临时文件被删掉且成品不受影响() {
        val dir = tempFolder.newFolder("tmp_files")
        val videoTmp = tempFolder.newFile("tmp_files/video_new.mp4")
        val audioTmp = tempFolder.newFile("tmp_files/audio_new.m4a")
        val finalVideo = tempFolder.newFile("tmp_files/video.mp4")

        FileUtil.cleanDownloadTempFiles(dir)

        assertFalse("video_new.mp4 是本次下载的临时文件", videoTmp.exists())
        assertFalse("audio_new.m4a 是本次下载的临时文件", audioTmp.exists())
        assertTrue("成品不能被顺手删掉", finalVideo.exists())
    }

    @Test
    fun 下载中标记被摘掉() {
        // 不摘掉的话本地列表会把这个目录一直当成"正在下载"，用户看不到已下好的内容
        val dir = tempFolder.newFolder("downloading")
        val mark = tempFolder.newFile("downloading/.DOWNLOADING")

        FileUtil.cleanDownloadTempFiles(dir)

        assertFalse(mark.exists())
    }

    @Test
    fun 不递归到子目录() {
        // 合集视频的每个分 P 是独立子目录、各有自己的任务记录：清父目录不能动别的任务正在写的文件
        val root = tempFolder.newFolder("multi")
        tempFolder.newFolder("multi", "P1")
        val subTmp = tempFolder.newFile("multi/P1/video_new.mp4")
        val subBak = tempFolder.newFile("multi/P1/video.mp4.bak")
        val subMark = tempFolder.newFile("multi/P1/.DOWNLOADING")

        FileUtil.cleanDownloadTempFiles(root)

        assertTrue("子目录里的临时文件属于别的任务，不能删", subTmp.exists())
        assertTrue("子目录里的 .bak 也不能动", subBak.exists())
        assertTrue("子目录里的下载中标记不能动", subMark.exists())
    }

    @Test
    fun 目录不存在或为null时不崩() {
        FileUtil.cleanDownloadTempFiles(null)
        FileUtil.cleanDownloadTempFiles(File(tempFolder.root, "not_there"))
    }
}
