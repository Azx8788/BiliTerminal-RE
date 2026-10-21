package com.RobinNotBad.BiliClient.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 跨会话去重的纯函数单测：合并顺序、容量上限、编解码容错。
 * 落盘部分走 SharedPreferences，不在这里覆盖（JVM 单测没有真实 Context）。
 */
class StorySeenStoreTest {

    @Test
    fun merge_keepsExistingOrderAndAppendsNewAids() {
        val merged = StorySeenStore.merge(setOf(1L, 2L), listOf(3L, 2L, 4L))

        assertEquals("已有顺序不变、新增追加、重复不重复计入", listOf(1L, 2L, 3L, 4L), merged.toList())
    }

    @Test
    fun merge_trimsOldestWhenExceedingMax() {
        val existing = (1L..(StorySeenStore.MAX_SIZE.toLong())).toSet()
        val merged = StorySeenStore.merge(existing, listOf(9999L))

        assertEquals("容量固定在上限", StorySeenStore.MAX_SIZE, merged.size)
        assertEquals("最旧的应被丢掉", 2L, merged.first())
        assertEquals("最新的应在尾部", 9999L, merged.last())
        assertTrue("最旧的那条不应再出现", 1L !in merged)
    }

    @Test
    fun encodeDecode_roundTrip() {
        val aids = listOf(10L, 20L, 30L)
        assertEquals(aids, StorySeenStore.decode(StorySeenStore.encode(aids)).toList())
    }

    @Test
    fun decode_brokenOrEmptyInput_returnsEmpty() {
        assertTrue("空串", StorySeenStore.decode("").isEmpty())
        assertTrue("null", StorySeenStore.decode(null).isEmpty())
        assertEquals(
            "非法片段跳过、非正数忽略",
            listOf(5L),
            StorySeenStore.decode(" 5 , abc, ,0,-3,").toList()
        )
    }

    @Test
    fun encode_emptyCollection_isEmptyString() {
        assertEquals("", StorySeenStore.encode(emptyList()))
    }
}
