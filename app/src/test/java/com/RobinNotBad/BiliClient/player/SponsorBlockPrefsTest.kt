package com.RobinNotBad.BiliClient.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * [SponsorBlockPrefs] 的纯 JVM 单测。
 *
 * 重点是「紧急下线」那条：[FEATURE_ENABLED] 为 false 时必须压过用户存档里的 true
 * （同 [SkipOpEdPrefs] 的教训——只改默认值对老用户无效）。
 */
class SponsorBlockPrefsTest {

    @Test
    fun isEnabled_requiresBothFeatureSwitchAndPref() {
        assertFalse("用户存档为关时一定不生效", SponsorBlockPrefs.isEnabled(false))
        assertEquals(
            "存档为开时，结果应等于功能总开关",
            SponsorBlockPrefs.FEATURE_ENABLED,
            SponsorBlockPrefs.isEnabled(true)
        )
    }

    @Test
    fun defaultCategories_areSponsorAndSelfPromo() {
        assertEquals(setOf("sponsor", "selfpromo"), SponsorBlockPrefs.DEFAULT_CATEGORIES)
    }
}