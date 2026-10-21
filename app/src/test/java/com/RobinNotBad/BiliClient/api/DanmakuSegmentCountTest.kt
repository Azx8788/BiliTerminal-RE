package com.RobinNotBad.BiliClient.api

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [DanmakuApi.segmentCountFor] 的纯 JVM 单测。
 *
 * 背景：分段数算错会直接改变用户看到的弹幕完整性——
 * - **少算** ⇒ 视频后半段整段没有弹幕；
 * - **多算** ⇒ 白请求（时长恰为 6 分钟整数倍时必发一次空包）；
 * - 时长未知（`duration` 还没就绪，回落成 3600 秒）时按 11 段请求，在手表上很贵。
 */
class DanmakuSegmentCountTest {

    @Test
    fun unknownDuration_fetchesSingleSegment() {
        assertEquals("时长未知不该按 3600 秒硬算 11 段", 1, DanmakuApi.segmentCountFor(0))
        assertEquals(1, DanmakuApi.segmentCountFor(-1))
    }

    @Test
    fun shortVideo_isOneSegment() {
        assertEquals(1, DanmakuApi.segmentCountFor(1))
        assertEquals(1, DanmakuApi.segmentCountFor(180))
        assertEquals("刚满一段时仍只需要 1 段", 1, DanmakuApi.segmentCountFor(360))
    }

    @Test
    fun exactMultiple_doesNotOverRequest() {
        // 旧实现 duration/360+1：720 秒会请求 3 段（第 3 段必然为空）
        assertEquals(2, DanmakuApi.segmentCountFor(720))
        assertEquals(3, DanmakuApi.segmentCountFor(1080))
        assertEquals(4, DanmakuApi.segmentCountFor(1440))
    }

    @Test
    fun boundary_coversLastSecond() {
        // 361 秒的视频在 360~361 处还有 1 秒弹幕，必须请求第 2 段，否则末尾一小段没有弹幕
        assertEquals(2, DanmakuApi.segmentCountFor(361))
        assertEquals(2, DanmakuApi.segmentCountFor(719))
        assertEquals(3, DanmakuApi.segmentCountFor(721))
    }

    @Test
    fun longVideo_coversWholeTimeline() {
        // 2 小时视频：7200/360 = 20 段，最后一段覆盖 6840~7200
        assertEquals(20, DanmakuApi.segmentCountFor(7200))
        assertEquals(21, DanmakuApi.segmentCountFor(7201))
    }

    @Test
    fun segmentSeconds_matchesBilibiliProtocol() {
        assertEquals("B 站 seg.so 一段 6 分钟", 360, DanmakuApi.SEGMENT_SECONDS)
    }
}
