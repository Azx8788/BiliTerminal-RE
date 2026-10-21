package com.RobinNotBad.BiliClient.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DanmakuSync] 的纯 JVM 单测。
 *
 * 钉住两条真实回归：
 * 1. 采样滞后的量级（250ms）不能触发吸附——否则每次采样都把 DFM 自走的时钟拽一次，滚动弹幕「一跳一跳」。
 * 2. **吸附（`IDanmakuView.seekTo`）会清空屏上弹幕**（`DrawTask.seek()` → `reset()`），
 *    所以小偏差绝不能吸附、吸附后必须有冷静期、连续不收敛必须退避——否则表现为
 *    「弹幕整屏消失 → 闪现零点几秒 → 又消失」。
 */
class DanmakuSyncTest {

    private val threshold = DanmakuSync.SNAP_THRESHOLD_MS

    /** 从最后一次吸附开始计时，便于断言"冷静期已过"。 */
    private fun decideAt(
        posMs: Long,
        timerMs: Long,
        elapsedSinceSnapMs: Long,
        consecutive: Int = 0
    ) = DanmakuSync.decide(
        posMs = posMs,
        timerMs = timerMs,
        nowMs = 1_000_000L + elapsedSinceSnapMs,
        lastSnapAtMs = 1_000_000L,
        consecutive = consecutive
    )

    @Test
    fun steadyStateStaleness_doesNotSnap() {
        // 采样间隔 250ms ⇒ 任意时刻位置最多滞后 250ms，稳态下必须一次都不吸附
        for (lag in 0L..250L) {
            assertFalse(
                "滞后 ${lag}ms 不应触发吸附（每次吸附都会清屏）",
                decideAt(1000L - lag, 1000L, 10_000L).snap
            )
        }
    }

    @Test
    fun withinThreshold_doesNotSnap() {
        assertFalse("位置与时钟一致时不需要吸附", decideAt(1000L, 1000L, 10_000L).snap)
        assertFalse("正向刚好等于阈值时不吸附", decideAt(1000L + threshold, 1000L, 10_000L).snap)
        assertFalse("负向刚好等于阈值时不吸附", decideAt(1000L - threshold, 1000L, 10_000L).snap)
    }

    @Test
    fun smallDrift_neverSnaps_evenWhenRepeated() {
        // 400ms 这类"看起来该修"的小偏差：宁可让弹幕错位一点，也不能反复清屏
        var consecutive = 0
        repeat(100) {
            val decision = decideAt(1400L, 1000L, it * 250L)
            consecutive = decision.consecutive
            assertFalse("400ms 偏差不在吸附阈值内（阈值=$threshold）", decision.snap)
        }
        assertEquals("从未吸附过，计数应保持 0", 0, consecutive)
    }

    @Test
    fun beyondThreshold_snaps() {
        assertTrue("时钟落后于视频超过阈值，应吸附", decideAt(1000L + threshold + 1, 1000L, 10_000L).snap)
        // 反向：视频在 0 时弹幕时钟已跑到 1.5s 之后（成片头起播时的典型漂移）
        assertTrue("时钟跑在视频前面超过阈值，应吸附", decideAt(0L, threshold + 1, 10_000L).snap)
    }

    @Test
    fun untrustedPosition_neverSnaps() {
        // 播放器未就绪/重建窗口期返回负数：此时灌进去会让整批弹幕被判定为"已过期"而不显示
        assertFalse(DanmakuSync.decide(-1L, 0L, 1_000_000L, 0L, 0).snap)
        assertFalse("位置不可信时，偏差再大也不能吸附", DanmakuSync.decide(-1L, 10_000L, 1_000_000L, 0L, 0).snap)
        assertFalse(DanmakuSync.decide(Long.MIN_VALUE, 0L, 1_000_000L, 0L, 0).snap)
    }

    @Test
    fun settleWindow_blocksImmediateSecondSnap() {
        val first = decideAt(10_000L, 0L, 100_000L)
        assertTrue("偏差 10s，第一次应吸附", first.snap)
        assertEquals(1, first.consecutive)

        // 刚吸附完 500ms 又采样到同样大的偏差：必须仍在冷静期内，不能第二次清屏
        val second = decideAt(10_250L, 0L, 500L, first.consecutive)
        assertFalse("吸附后 ${DanmakuSync.SETTLE_MS}ms 内不得再次吸附", second.snap)
        assertEquals("冷静期内计数不增长", 1, second.consecutive)

        // 冷静期过去后允许再试
        val third = decideAt(10_500L, 0L, DanmakuSync.SETTLE_MS, first.consecutive)
        assertTrue("冷静期结束后允许再次吸附", third.snap)
        assertEquals(2, third.consecutive)
    }

    @Test
    fun repeatedSnaps_backOff() {
        // 连续吸附到上限后，间隔从 SETTLE_MS 升级为 BACKOFF_MS
        assertEquals(DanmakuSync.SETTLE_MS, DanmakuSync.settleFor(0))
        assertEquals(DanmakuSync.SETTLE_MS, DanmakuSync.settleFor(DanmakuSync.MAX_CONSECUTIVE_SNAPS - 1))
        assertEquals(DanmakuSync.BACKOFF_MS, DanmakuSync.settleFor(DanmakuSync.MAX_CONSECUTIVE_SNAPS))

        val backedOff = decideAt(
            posMs = 60_000L,
            timerMs = 0L,
            elapsedSinceSnapMs = DanmakuSync.SETTLE_MS + 1,
            consecutive = DanmakuSync.MAX_CONSECUTIVE_SNAPS
        )
        assertFalse("已连续吸附多次仍不收敛：必须退避，不能再每秒清屏", backedOff.snap)

        val afterBackoff = decideAt(
            posMs = 60_000L,
            timerMs = 0L,
            elapsedSinceSnapMs = DanmakuSync.BACKOFF_MS,
            consecutive = DanmakuSync.MAX_CONSECUTIVE_SNAPS
        )
        assertTrue("退避期满后允许再试一次", afterBackoff.snap)
    }

    @Test
    fun converged_resetsCounter() {
        val recovered = decideAt(1000L, 1000L, 10L, consecutive = 5)
        assertFalse(recovered.snap)
        assertEquals("偏差回到阈值内，连续吸附计数必须清零（否则退避会一直挂着）", 0, recovered.consecutive)
    }

    @Test
    fun threshold_isFarAboveSamplingLag() {
        assertTrue(
            "阈值必须远大于 250ms 的采样间隔，否则每次采样都可能触发清屏",
            threshold >= DanmakuSync.SAMPLE_INTERVAL_MS * 4
        )
    }
}
