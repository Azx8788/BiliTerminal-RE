package com.RobinNotBad.BiliClient.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SponsorBlockSkip] 的纯 JVM 单测。
 *
 * 钉住的几条真实会翻车的边界：
 * 1. 区间左闭右开——跳过之后位置正好落在 `toSec`，右闭会让定时器每轮都再跳一次；
 * 2. 首尾相接/重叠的两段必须合并成一次 seek——否则进度条会连跳两次；
 * 3. 已处理过的片段不再跳——用户点了「撤回」往回拖，不能被立刻弹走；
 * 4. cid 不符的条目必须丢掉——多分P 视频的片段都挂在同一个 videoID 下返回。
 */
class SponsorBlockSkipTest {

    private val cats = setOf("sponsor", "selfpromo")

    private fun seg(from: Double, to: Double, uuid: String = "", category: String = "sponsor", cid: String = "111") =
        SponsorBlockSkip.Segment(uuid, category, from, to, cid, 1)

    @Test
    fun buildSegments_filtersByEveryRule() {
        val raw = """
            [
              {"segment":[100.0,200.0],"UUID":"a","category":"sponsor","actionType":"skip","votes":5,"cid":"111"},
              {"segment":[200.0,300.0],"UUID":"b","category":"selfpromo","actionType":"skip","votes":0,"cid":"111"},
              {"segment":[400.0,401.5],"UUID":"c","category":"interaction","actionType":"skip","votes":1,"cid":"111"},
              {"segment":[500.0,500.5],"UUID":"d","category":"sponsor","actionType":"skip","votes":1,"cid":"111"},
              {"segment":[600.0,1600.0],"UUID":"e","category":"sponsor","actionType":"skip","votes":1,"cid":"111"},
              {"segment":[700.0,800.0],"UUID":"f","category":"sponsor","actionType":"mute","votes":1,"cid":"111"},
              {"segment":[900.0,1000.0],"UUID":"g","category":"sponsor","actionType":"skip","votes":-2,"cid":"111"},
              {"segment":[1100.0,1200.0],"UUID":"h","category":"sponsor","actionType":"skip","votes":1,"cid":"222"}
            ]
        """.trimIndent()

        val result = SponsorBlockSkip.buildSegments(raw, cats, "111")

        // 只留下 a、b；其余分别被类别/时长过短/过长/actionType/负票/cid 挡掉
        assertEquals(2, result.size)
        assertEquals(listOf("a", "b"), result.map { it.uuid })
    }

    @Test
    fun buildSegments_sortsByStart() {
        val raw = """
            [
              {"segment":[900.0,950.0],"UUID":"late","category":"sponsor","actionType":"skip","votes":1,"cid":"111"},
              {"segment":[100.0,150.0],"UUID":"early","category":"sponsor","actionType":"skip","votes":1,"cid":"111"}
            ]
        """.trimIndent()

        assertEquals(listOf("early", "late"), SponsorBlockSkip.buildSegments(raw, cats, "111").map { it.uuid })
    }

    @Test
    fun buildSegments_keepsOtherCidWhenCurrentUnknown() {
        val raw = """[{"segment":[10.0,20.0],"UUID":"x","category":"sponsor","actionType":"skip","votes":1,"cid":"222"}]"""
        assertEquals(1, SponsorBlockSkip.buildSegments(raw, cats, null).size)
    }

    @Test
    fun buildSegments_toleratesGarbage() {
        assertTrue(SponsorBlockSkip.buildSegments("", cats, "111").isEmpty())
        assertTrue(SponsorBlockSkip.buildSegments("not json", cats, "111").isEmpty())
        assertTrue(SponsorBlockSkip.buildSegments("[]", cats, "111").isEmpty())
        // 类别表为空时整段不跳
        assertTrue(SponsorBlockSkip.buildSegments("""[{"segment":[1.0,2.0],"category":"sponsor","actionType":"skip"}]""", emptySet(), "111").isEmpty())
    }

    @Test
    fun segmentAt_isLeftClosedRightOpen() {
        val segments = listOf(seg(100.0, 200.0, "a"))
        assertEquals("a", SponsorBlockSkip.segmentAt(100.0, segments)?.uuid)
        assertEquals("a", SponsorBlockSkip.segmentAt(199.9, segments)?.uuid)
        assertNull(SponsorBlockSkip.segmentAt(200.0, segments))
        assertNull(SponsorBlockSkip.segmentAt(99.9, segments))
        assertNull(SponsorBlockSkip.segmentAt(-1.0, segments))
    }

    @Test
    fun skipChainAt_mergesTouchingSegments() {
        val segments = listOf(seg(100.0, 200.0, "a"), seg(200.0, 300.0, "b"))
        val chain = SponsorBlockSkip.skipChainAt(150.0, segments, emptySet())!!
        assertEquals(listOf("a", "b"), chain.map { it.uuid })
        assertEquals(300.0, SponsorBlockSkip.targetOf(chain, null), 0.001)
    }

    @Test
    fun skipChainAt_mergesOverlappingSegments() {
        val segments = listOf(seg(100.0, 250.0, "a"), seg(200.0, 300.0, "b"))
        val chain = SponsorBlockSkip.skipChainAt(120.0, segments, emptySet())!!
        assertEquals(listOf("a", "b"), chain.map { it.uuid })
    }

    @Test
    fun skipChainAt_ignoresHandledSegment() {
        val segments = listOf(seg(100.0, 200.0, "a"))
        // 用户在「撤回」后回到 a 之中：a 已处理，不能再弹
        assertNull(SponsorBlockSkip.skipChainAt(150.0, segments, setOf("a")))
    }

    @Test
    fun skipChainAt_nothingAtPosition() {
        val segments = listOf(seg(100.0, 200.0, "a"))
        assertNull(SponsorBlockSkip.skipChainAt(50.0, segments, emptySet()))
        assertNull(SponsorBlockSkip.skipChainAt(200.0, segments, emptySet()))
    }

    @Test
    fun targetOf_clampsToDuration() {
        val chain = listOf(seg(100.0, 300.0, "a"))
        assertEquals(300.0, SponsorBlockSkip.targetOf(chain, 1000.0), 0.001)
        // 片段数据过期：视频实际只有 250 秒，链尾 300 越界，钳到 249.8
        assertEquals(249.8, SponsorBlockSkip.targetOf(chain, 250.0), 0.001)
        assertEquals(300.0, SponsorBlockSkip.targetOf(chain, null), 0.001)
    }

    @Test
    fun keyForManualSeek_marksOnlyFreshSegment() {
        val segments = listOf(seg(100.0, 200.0, "a"))
        assertEquals("a", SponsorBlockSkip.keyForManualSeek(150.0, segments, emptySet()))
        assertNull(SponsorBlockSkip.keyForManualSeek(150.0, segments, setOf("a")))
        assertNull(SponsorBlockSkip.keyForManualSeek(50.0, segments, emptySet()))
    }

    @Test
    fun keyOf_fallsBackWhenUuidMissing() {
        val noUuid = seg(100.0, 200.0, "")
        assertEquals("sponsor@100.0-200.0", SponsorBlockSkip.keyOf(noUuid))
    }
}