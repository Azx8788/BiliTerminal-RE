package com.RobinNotBad.BiliClient.util.view

import com.RobinNotBad.BiliClient.util.Logu

/**
 * 「回车把焦点往下推」这条平台逻辑的兜底（线上崩溃 #66，见 `docs/architecture-map.md` §7.50）。
 *
 * <h3>崩溃是什么</h3>
 * `TextView.onKeyUp` 收到 `KEYCODE_ENTER`（且 `event.hasNoModifiers()`、该 TextView 没有
 * OnClickListener）时，只要 `(event.getFlags() & FLAG_EDITOR_ACTION) != 0` 或
 * `shouldAdvanceFocusOnEnter()` 为真（**单行**输入框，或文本类里 variation 是
 * EMAIL_ADDRESS / EMAIL_SUBJECT），就会 `focusSearch(FOCUS_DOWN)`；找到候选视图后
 * `requestFocus(FOCUS_DOWN)` 一失败，**平台自己直接抛**
 * `IllegalStateException("focus search returned a view that wasn't able to take focus!")`。
 *
 * 这是平台前后判据不一致：找候选那一步只按 `focusableInTouchMode` 过筛（`View.addFocusables`），
 * 而真正给焦点时 `requestFocusNoSearch()` 还会拒掉「触摸模式下不是 focusableInTouchMode」以及
 * 「祖先 `descendantFocusability=blocksDescendants` / `shouldBlockFocusForTouchscreen()`」的候选
 * （android-8.1.0_r1 `View.java:10525-10538` 与 `10850-10870`）。应用侧改不了平台判据，只能兜住。
 *
 * <h3>为什么兜在 dispatchKeyEvent</h3>
 * 输入框在全工程有十几处（布局里的 + 代码 new 的），漏一处就还有崩溃；而且这条 throw 取决于
 * 运行时焦点、触摸模式、视图层级，是**输入框周边环境**的问题，不是某个输入框的问题。
 * `dispatchKeyEvent` 是按键进 Activity 的必经之路（`DecorView.dispatchKeyEvent` →
 * `Activity.dispatchKeyEvent` → `PhoneWindow.superDispatchKeyEvent` → 视图树），在异常冒泡的
 * 路上兜住，覆盖面最广、对正常按键**零行为变化**。
 *
 * <h3>没覆盖到的地方（已知）</h3>
 * 只兜「按键分发经过 Activity」的窗口。`TerminalDialog` 造出来的 `AlertDialog` 是独立窗口，它的
 * 按键分发不经过 Activity（`Dialog` 实现 `Window.Callback`，但 `DecorView` 先
 * `superDispatchKeyEvent` 再回调），给 Dialog 换 callback 也**不能保证**在崩溃前拦到；而
 * `setOnEditorActionListener` 一类「让平台不进那分支」的改法会改掉多行输入框的回车行为，属未验证的
 * 行为变更，本次都没做。`util/TerminalDialog.input()` 全仓只有 2 处调用（反馈回复、开发者工具），
 * 线上从未出现弹窗内回车导致的该崩溃，故记为已知缺口而非已修。
 * 另：`InputDialogActivity` 是普通 `BaseActivity` 页面，天然被本兜底覆盖。
 *
 * <h3>注意</h3>
 * - **软键盘的「完成 / 下一项」也被本兜底覆盖**：`BaseInputConnection.performEditorAction` 不直接调
 *   `TextView.onEditorAction(int)`，而是合成一对带 `FLAG_EDITOR_ACTION` 的 ENTER（DONE 也一样），
 *   经 `InputMethodManager.dispatchKeyEventFromInputMethod` → `ViewRootImpl.dispatchKeyFromIme` 入队，
 *   最终和物理按键**同一条分发栈**（所以崩溃栈里分不出是手按还是 IME 按的）。
 * - 真正漏网的只有「显式调 `TextView.onEditorAction(IME_ACTION_NEXT/PREVIOUS)`」：8.1 在
 *   `TextView.java:5926`/`5936`、12 在 `7164`/`7174` 也抛同一句 ISE，但框架内只有无障碍
 *   `accessibilityActionImeEnter` 直接调它，线上未出现过，未在本次范围。
 * - 只认消息原文，不做 `IllegalStateException` 一把抓：同名的 ISE 在别处可能是真 bug，
 *   吞掉等于把问题藏起来。平台这句话是**字面量常量**（不本地化），可以精确匹配。
 */
object EditorEnterKeyGuard {

    /**
     * 平台在 `TextView.onKeyUp` 里抛的那句原文（android-8.1.0_r1 `TextView.java:7453`，
     * 12.0.0_r1 同文，不是本地化字符串）。
     */
    const val FOCUS_SEARCH_FAILURE_MESSAGE =
        "focus search returned a view that wasn't able to take focus!"

    /** 异常链最多往下翻几层：平台这条例外不会包深，8 层足够，也避免被循环 cause 拖死。 */
    private const val MAX_CAUSE_DEPTH = 8

    /**
     * 这个异常（含 cause 链）是不是平台那条「回车推进焦点失败」的 ISE。
     *
     * 纯函数，JVM 单测直接断言（见 `app/src/test/.../util/view/EditorEnterKeyGuardTest.kt`）。
     */
    @JvmStatic
    fun isFocusSearchFailure(e: Throwable?): Boolean {
        var current = e
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (current is IllegalStateException && current.message == FOCUS_SEARCH_FAILURE_MESSAGE) return true
            current = current.cause
            depth++
        }
        return false
    }

    /**
     * `dispatchKeyEvent` 里的兜底：把平台这条 ISE 记日志并吞掉，其余异常原样返回 false（调用方重抛）。
     *
     * @return true = 已兜住（调用方应把这条按键当成已消费）
     */
    @JvmStatic
    fun swallowFocusSearchFailure(tag: String, e: Throwable): Boolean {
        if (!isFocusSearchFailure(e)) return false
        // 记一条 e 级日志：这条崩溃的频次取决于用户按不按回车/软键盘完成键，
        // 但每条都带页面 tag，排查时能直接定位是哪个页面触发的。
        Logu.e(tag, "平台回车焦点推进崩溃已兜住（focus search / requestFocus 判据不一致）：${e.message}")
        return true
    }
}
