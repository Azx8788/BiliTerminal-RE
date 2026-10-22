package com.RobinNotBad.BiliClient.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [BvidCodec] 的纯 JVM 单测。
 *
 * 钉的是「已知向量」：算法一旦被误改（比如把交换写成赋值），产出的 BV 号全错，
 * 但编译和 lint 都不报错——只有拿真实 av/BV 对来对才知道。
 */
class BvidCodecTest {

    @Test
    fun av2bv_matchesKnownVectors() {
        assertEquals("BV17x411w7KC", BvidCodec.av2bv(170001L))
        assertEquals("BV1xx411c7mD", BvidCodec.av2bv(2L))
        assertEquals("BV1Q541167Qg", BvidCodec.av2bv(455017605L))
        // SponsorBlock 上一个真实收录的视频
        assertEquals("BV1bY4y1v7Mb", BvidCodec.av2bv(637828183L))
    }

    @Test
    fun bv2av_matchesKnownVectors() {
        assertEquals(170001L, BvidCodec.bv2av("BV17x411w7KC")!!)
        assertEquals(2L, BvidCodec.bv2av("BV1xx411c7mD")!!)
        assertEquals(455017605L, BvidCodec.bv2av("BV1Q541167Qg")!!)
        assertEquals(637828183L, BvidCodec.bv2av("BV1bY4y1v7Mb")!!)
    }

    @Test
    fun roundTrip_isStable() {
        for (aid in listOf(1L, 2L, 170001L, 455017605L, 637828183L, 999999999L)) {
            val bvid = BvidCodec.av2bv(aid)
            assertEquals("aid=$aid 往返后应还原", aid, BvidCodec.bv2av(bvid)!!)
        }
    }

    @Test
    fun bv2av_rejectsMalformedInput() {
        assertNull(BvidCodec.bv2av(""))
        assertNull(BvidCodec.bv2av("BV17x411w7K"))     // 只有 11 位
        assertNull(BvidCodec.bv2av("AV17x411w7KC"))    // 前缀不对
        assertNull(BvidCodec.bv2av("BV17x411w7K0"))    // 末位是表外字符 '0'
    }
}