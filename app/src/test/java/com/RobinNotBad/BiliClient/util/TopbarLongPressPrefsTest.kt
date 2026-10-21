package com.RobinNotBad.BiliClient.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 「长按顶部标题栏」三模式的默认值、判据与旧开关迁移。
 *
 * 钉住四件**不会崩、只会静默错**的事：
 *
 * 1. **默认必须是「全部页面」**——这个手势本来就在线上跑着，三模式若默认选「不响应」，
 *    等于悄悄改掉了所有老用户的操作习惯，而且没有任何提示。
 * 2. **旧布尔开关必须能迁移**——26.10.19 显式关过开关的用户，升级后应落到「仅视频详情页」
 *    （当年关开关时视频详情页本来就没被关掉），既不能突然全部不响应，也不能被默认值重新打开。
 * 3. **新键优先于旧键**——用户在新版本里选过模式之后，旧键的残留值不能再把行为拽回去。
 * 4. **键名稳定、且不能与「长按打开操作面板」共用**——共用键会让两个完全无关的手势互相影响。
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
    fun 默认模式是全部页面() {
        assertEquals(TopbarLongPressPrefs.MODE_ALL, TopbarLongPressPrefs.DEFAULT_MODE)
    }

    @Test
    fun 从未存过时两个页面都响应() {
        assertEquals("新键与旧键都没写过时应取默认值 all", TopbarLongPressPrefs.MODE_ALL, TopbarLongPressPrefs.mode())
        assertTrue("普通页面应打开菜单", TopbarLongPressPrefs.isEnabledForNormalPages())
        assertTrue("视频详情页应关闭所有详情页", TopbarLongPressPrefs.isEnabledForVideoPages())
    }

    @Test
    fun 不响应模式下两个页面都不响应() {
        SharedPreferencesUtil.putString(SettingsKeys.TOPBAR_LONG_PRESS_MODE, TopbarLongPressPrefs.MODE_OFF)
        assertEquals(TopbarLongPressPrefs.MODE_OFF, TopbarLongPressPrefs.mode())
        assertFalse(TopbarLongPressPrefs.isEnabledForNormalPages())
        assertFalse(TopbarLongPressPrefs.isEnabledForVideoPages())
    }

    @Test
    fun 仅视频详情页模式只放行视频详情页() {
        SharedPreferencesUtil.putString(SettingsKeys.TOPBAR_LONG_PRESS_MODE, TopbarLongPressPrefs.MODE_VIDEO_ONLY)
        assertFalse("普通页面不应再打开菜单", TopbarLongPressPrefs.isEnabledForNormalPages())
        assertTrue("视频详情页仍应关闭所有详情页", TopbarLongPressPrefs.isEnabledForVideoPages())
    }

    @Test
    fun 全部页面模式下两个页面都响应() {
        SharedPreferencesUtil.putString(SettingsKeys.TOPBAR_LONG_PRESS_MODE, TopbarLongPressPrefs.MODE_ALL)
        assertTrue(TopbarLongPressPrefs.isEnabledForNormalPages())
        assertTrue(TopbarLongPressPrefs.isEnabledForVideoPages())
    }

    /** 26.10.19 的「关」= 普通页面不弹菜单、视频详情页照旧，正好是现在的「仅视频详情页」。 */
    @Test
    fun 旧开关关过之后迁移成仅视频详情页() {
        SharedPreferencesUtil.putBoolean(SettingsKeys.TOPBAR_LONG_PRESS_MENU_ENABLE, false)
        assertEquals(TopbarLongPressPrefs.MODE_VIDEO_ONLY, TopbarLongPressPrefs.mode())
        assertFalse(TopbarLongPressPrefs.isEnabledForNormalPages())
        assertTrue(TopbarLongPressPrefs.isEnabledForVideoPages())
    }

    @Test
    fun 旧开关开过之后仍然是全部页面() {
        SharedPreferencesUtil.putBoolean(SettingsKeys.TOPBAR_LONG_PRESS_MENU_ENABLE, true)
        assertEquals(TopbarLongPressPrefs.MODE_ALL, TopbarLongPressPrefs.mode())
    }

    /** 新版本里选过模式之后，旧键的残留值不能再影响行为。 */
    @Test
    fun 新键优先于旧开关() {
        SharedPreferencesUtil.putBoolean(SettingsKeys.TOPBAR_LONG_PRESS_MENU_ENABLE, false)
        SharedPreferencesUtil.putString(SettingsKeys.TOPBAR_LONG_PRESS_MODE, TopbarLongPressPrefs.MODE_ALL)
        assertEquals(TopbarLongPressPrefs.MODE_ALL, TopbarLongPressPrefs.mode())
        assertTrue(TopbarLongPressPrefs.isEnabledForNormalPages())
    }

    /** 新键里是脏值（手改存档 / 回滚过版本）时按默认值走，别让界面显示成半残状态。 */
    @Test
    fun 新键是脏值时回退到默认() {
        SharedPreferencesUtil.putString(SettingsKeys.TOPBAR_LONG_PRESS_MODE, "whatever")
        assertEquals(TopbarLongPressPrefs.MODE_ALL, TopbarLongPressPrefs.mode())
    }

    /** 键名不能被改掉：改了等于把老用户的设置读成「没设过」，默认值会把他关掉的开关重新打开。 */
    @Test
    fun 键名是稳定的() {
        assertEquals("topbar_long_press_mode", SettingsKeys.TOPBAR_LONG_PRESS_MODE)
        assertEquals("topbar_long_press_menu_enable", SettingsKeys.TOPBAR_LONG_PRESS_MENU_ENABLE)
    }

    /** 两个长按手势是两件事，键不能撞。 */
    @Test
    fun 不与长按操作面板共用键() {
        assertNotEquals(
            "顶栏长按与正文长按操作面板必须各用各的键",
            SettingsKeys.LONG_PRESS_PANEL_ENABLE,
            SettingsKeys.TOPBAR_LONG_PRESS_MODE
        )
    }
}
