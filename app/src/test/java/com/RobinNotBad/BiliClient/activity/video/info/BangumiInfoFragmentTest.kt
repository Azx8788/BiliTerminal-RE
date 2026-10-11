package com.RobinNotBad.BiliClient.activity.video.info

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 选集标题行状态文案的优先级测试（批次 4：状态文案 + 全量选集弹窗）。
 *
 * 文案取错会让用户看到与当前季不符的状态（典型：切到剧场版仍显示主季的"连载中"），
 * 所以优先级顺序本身就是这段逻辑的契约。只测纯函数，不碰视图。
 */
class BangumiInfoFragmentTest {

    @Test
    fun 当前季有desc_优先用它而不是详情desc() {
        assertEquals(
            "更新至第13话",
            BangumiInfoFragment.episodeStatusText("更新至第13话", "连载中，共24话", 0, 24)
        )
    }

    @Test
    fun 当前季没desc_退回详情desc() {
        assertEquals(
            "连载中，共24话",
            BangumiInfoFragment.episodeStatusText("", "连载中，共24话", 0, 24)
        )
        assertEquals(
            "连载中，共24话",
            BangumiInfoFragment.episodeStatusText(null, "连载中，共24话", 0, 24)
        )
    }

    @Test
    fun 两个desc都没有_按完结状态与正片集数拼() {
        assertEquals("已完结，共12话", BangumiInfoFragment.episodeStatusText(null, null, 1, 12))
        assertEquals("连载中，共3话", BangumiInfoFragment.episodeStatusText(null, null, 0, 3))
    }

    @Test
    fun 连正片集数都没有_只说选集() {
        assertEquals("选集", BangumiInfoFragment.episodeStatusText(null, null, 1, 0))
        assertEquals("选集", BangumiInfoFragment.episodeStatusText("", "", 0, 0))
    }
}
