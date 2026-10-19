package com.RobinNotBad.BiliClient.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 「长按顶部标题栏打开菜单」开关的默认值与键名。
 *
 * 钉住三件**不会崩、只会静默错**的事：
 *
 * 1. **默认必须是开启**——这个手势本来就在线上跑着，新开关若默认关掉，等于悄悄改掉了
 *    所有老用户的操作习惯，而且没有任何提示。
 * 2. **老用户显式关过的必须还是关的**——升级覆盖安装不能把人家关掉的开关又打开，
 *    靠的是 `getBoolean(key, def)` 的语义（键不存在才用 def）。
 * 3. **键名稳定、且不能与「长按打开操作面板」共用**——共用键会让两个完全无关的手势
 *    互相影响：关掉顶栏长按，评论长按面板也跟着没了。
 */
class TopbarLongPressPrefsTest {

    @Before
    fun setUp() {
        SharedPreferencesUtil.sharedPreferences = FakeSharedPreferences()
    }

    @After
    fun tearDown() {
        SharedPreferencesUtil.sharedPreferences = null
    }

    @Test
    fun 默认值是开启() {
        assertTrue("长按顶部打开菜单必须默认开启（保持现状）", TopbarLongPressPrefs.DEFAULT_ENABLED)
    }

    @Test
    fun 从未存过时得到开启() {
        assertTrue("键不存在时应取默认值 true", TopbarLongPressPrefs.isEnabled())
    }

    @Test
    fun 老用户显式关过之后仍然是关闭() {
        SharedPreferencesUtil.putBoolean(SettingsKeys.TOPBAR_LONG_PRESS_MENU_ENABLE, false)
        assertFalse("用户显式关过的设置不能被默认值覆盖掉", TopbarLongPressPrefs.isEnabled())
    }

    @Test
    fun 老用户显式开过之后仍然是开启() {
        SharedPreferencesUtil.putBoolean(SettingsKeys.TOPBAR_LONG_PRESS_MENU_ENABLE, true)
        assertTrue(TopbarLongPressPrefs.isEnabled())
    }

    /** 键名不能被改掉：改了等于把老用户的设置读成「没设过」，默认值会把他关掉的开关重新打开。 */
    @Test
    fun 键名是稳定的() {
        assertEquals("topbar_long_press_menu_enable", SettingsKeys.TOPBAR_LONG_PRESS_MENU_ENABLE)
    }

    /** 两个长按手势是两件事，键不能撞。 */
    @Test
    fun 不与长按操作面板共用键() {
        assertNotEquals(
            "顶栏长按与正文长按操作面板必须各用各的键",
            SettingsKeys.LONG_PRESS_PANEL_ENABLE,
            SettingsKeys.TOPBAR_LONG_PRESS_MENU_ENABLE
        )
    }
}
