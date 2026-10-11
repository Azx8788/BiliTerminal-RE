package com.RobinNotBad.BiliClient.adapter.video

import com.RobinNotBad.BiliClient.adapter.video.EpisodeCardAdapter.BadgeKind
import org.junit.Assert.assertEquals
import org.junit.Test

/** 选集角标归类：服务端给的是展示文案而不是枚举，认错会让"会员"那集看着像免费的。 */
class EpisodeCardAdapterTest {

    @Test
    fun badgeKind_会员_归为VIP() {
        assertEquals(BadgeKind.VIP, EpisodeCardAdapter.badgeKind("会员"))
    }

    @Test
    fun badgeKind_限免_归为FREE() {
        assertEquals(BadgeKind.FREE, EpisodeCardAdapter.badgeKind("限免"))
    }

    @Test
    fun badgeKind_其它文案_归为OTHER() {
        assertEquals(BadgeKind.OTHER, EpisodeCardAdapter.badgeKind("独家"))
        assertEquals(BadgeKind.OTHER, EpisodeCardAdapter.badgeKind("预告"))
    }

    @Test
    fun badgeKind_空或null_归为NONE_不显示角标() {
        assertEquals(BadgeKind.NONE, EpisodeCardAdapter.badgeKind(null))
        assertEquals(BadgeKind.NONE, EpisodeCardAdapter.badgeKind(""))
    }
}
