package com.RobinNotBad.BiliClient.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 锁住 [FileUtil.canWriteDirectory] 的判据：**真的去试写**，而不是看授权状态。
 *
 * 背景（issue #42）：默认下载目录 `<外部存储>/Android/media/<包名>/` 是应用专属外部存储，
 * Android 10+ 写这里不需要任何权限，但旧实现用 `Environment.isExternalStorageManager()`
 * 一律判成「没权限」，于是每次下载都把用户顶到系统设置页；手表 ROM 没有那个设置页，
 * 用户永远授不到权限，默认路径下的下载被永久挡死。
 *
 * 这个类只碰 `java.io.File`，因此可以在纯 JVM 单测里跑（不依赖 Android 运行时）。
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
}
