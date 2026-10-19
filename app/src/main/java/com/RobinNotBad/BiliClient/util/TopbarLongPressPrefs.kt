package com.RobinNotBad.BiliClient.util

/**
 * 「长按顶部标题栏打开菜单列表」的默认值与判据。
 *
 * <h3>这个开关解决什么</h3>
 * `BaseActivity.setupTopbarLongPressToHome` 给绝大多数页面补了「长按顶栏回主菜单」，
 * 但这个监听器挂在**整条顶栏**上（`R.id.top`，含左侧页面名、右侧时间），
 * 而菜单列表页（`MenuActivity`）自身又被排除在外——于是「除菜单列表外任何地方长按顶部都会弹菜单」，
 * 用户反馈误触很频繁。开关关掉后，长按顶栏在手势层就被吞掉，不再跳转。
 *
 * 这个开关只管本页这条「长按顶栏 → 菜单列表」手势。视频详情页那条「长按关闭所有详情页」
 * （`VideoInfoActivity.setupLongPressToRoot`）是另一套手势，不受本开关影响。
 *
 * <h3>为什么默认值收在这里</h3>
 * 与 `LongPressPrefs` / `player/SkipOpEdPrefs` 同样的理由：默认值一旦散落在「设置页写的」
 * 和「监听器里读的」两处，改漏一处的症状是「设置页显示开着、实际却是关的」——这种错不报错、
 * 只能靠人肉比对发现。设置页必须引用 [DEFAULT_ENABLED]，不要再写 `"true"` 字面量。
 */
object TopbarLongPressPrefs {

    /** `SettingsKeys.TOPBAR_LONG_PRESS_MENU_ENABLE` 的默认值：开启（保持本功能上线前的样子）。 */
    const val DEFAULT_ENABLED = true

    /**
     * 当前是否开启。
     *
     * 语义同 `SharedPreferencesUtil.getBoolean(key, def)`：**键不存在才用默认值**，
     * 所以老用户显式关过之后不会被默认值重新打开。
     *
     * 调用点是**长按按下那一刻**，不是页面 onStart：用户可能在别的页面改完开关再回来，
     * 监听器不需要重装，这里读到的就是最新值。
     */
    @JvmStatic
    fun isEnabled(): Boolean =
        SharedPreferencesUtil.getBoolean(SettingsKeys.TOPBAR_LONG_PRESS_MENU_ENABLE, DEFAULT_ENABLED)
}
