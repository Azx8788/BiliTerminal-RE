package com.RobinNotBad.BiliClient.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import com.RobinNotBad.BiliClient.util.TimeUtil

/**
 * 显示"时:分"并在整分钟自刷新的 TextView。
 *
 * <p><b>为什么不用 {@code @JvmOverloads} 主构造</b>：minSdk 19 下绝不能调
 * {@code TextView(context, attrs, defStyleAttr, defStyleRes)} 这个 4 参构造，它是 API 21 才有的。
 * 而 `@JvmOverloads` 生成的 2 参构造（XML inflate 用的那个）方法体里调的正是它 ——
 * 4.4 上一被布局解析就会崩。改写成三个显式次构造之后，每个都只碰 API 1 就存在的重载。
 */
@SuppressLint("AppCompatCustomView")
class TextClock : TextView {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private var stopped = false

    fun init() {
        typeface = Typeface.DEFAULT_BOLD
    }

    private val ticker = object : Runnable {
        override fun run() {
            removeCallbacks(this)
            if (stopped) return

            val now = System.currentTimeMillis()
            text = TimeUtil.formatTime(now)

            val next = 60000 - now % 60000
            postDelayed(this, next)
        }
    }

    fun startTick() {
        stopped = false
        ticker.run()
    }

    fun stopTick() {
        stopped = true
        removeCallbacks(ticker)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTick()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTick()
    }

    override fun onScreenStateChanged(screenState: Int) {
        super.onScreenStateChanged(screenState)

        if (screenState == SCREEN_STATE_ON) startTick()
        else stopTick()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)

        if (isVisible) startTick()
        else stopTick()
    }
}