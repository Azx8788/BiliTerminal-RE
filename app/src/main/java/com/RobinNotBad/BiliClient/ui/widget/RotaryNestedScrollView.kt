package com.RobinNotBad.BiliClient.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView
import com.RobinNotBad.BiliClient.util.SettingsKeys

/**
 * 支持表冠滚动的 `NestedScrollView`。
 *
 * 表冠判定与滚动逻辑见 [RotaryEncoderSupport]（三份重复实现已合并到那里）。
 * 与另外两个控件的两点差异原样保留：
 * 1. 走 `dispatchGenericMotionEvent` 覆写而不是 `setOnGenericMotionListener`；
 * 2. 滚动后**不**抢焦点。
 *
 * 开关与灵敏度改成每次事件现读（26.10.17），理由同 [RotaryScrollView]。
 */
class RotaryNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = RotaryEncoderSupport.handle(
            this, event,
            RotaryEncoderSupport.multipleOf(SettingsKeys.UI_ROTATORY_SCROLL),
            requestFocus = false
        ) { smoothScrollBy(0, it) }
        if (handled) return true
        return super.dispatchGenericMotionEvent(event)
    }
}
