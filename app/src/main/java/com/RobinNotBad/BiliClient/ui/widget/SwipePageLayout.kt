package com.RobinNotBad.BiliClient.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.SeekBar
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * 横向翻页页面的根布局：**方向判定 + 跟手拖动**。
 *
 * 为什么不用外层 ViewPager2 自带的滑动？它判定「横向」的阈值很松，竖向刷短视频时
 * 手指稍微一飘就会被它拦截，表现为「随便一划就翻到详情页」。这里改成由每一页自己判：
 *
 * 1. 手指移动超过最小滑动距离后，只有**横向位移明显大于竖向（2 倍以上）**才接管；
 *    竖向、斜向、多指（缩放）一律放给子 View，所以上下刷视频/滚评论不会被误判；
 * 2. 子 View 调用了 `requestDisallowInterceptTouchEvent(true)` 时（例如拖进度条），
 *    ViewGroup 不会回调 [onInterceptTouchEvent]，同样不会被误判；
 * 3. 接管后用 [ViewPager2.beginFakeDrag]/[ViewPager2.fakeDragBy] 让外壳 pager
 *    跟着手指走，松手 [ViewPager2.endFakeDrag] 再吸附到整页——和原生 ViewPager 手感一致，
 *    不是「松手后突然跳页」。
 *
 * 外壳 pager 本身要 `isUserInputEnabled = false`，否则两套滑动会打架。
 */
class SwipePageLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 手指向左（-1）/ 向右（1）滑过阈值时回调；本页不知道页序，由宿主决定翻到哪。 */
    var onSwipe: ((direction: Int) -> Unit)? = null

    /** 外壳 pager，用于跟手拖动；拿不到就退化成「松手切页」。 */
    var pagerProvider: (() -> ViewPager2?)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var startItem = 0
    private var horizontal = false
    private var decided = false
    private var multiTouch = false
    private var fakeDragging = false

    /** 按下点落在进度条这类「自己要横向拖动」的控件上：本次手势绝不翻页。 */
    private var blockHorizontal = false

    /**
     * 子 View（滚动容器、RecyclerView）会调 true 请求父级别拦截。
     *
     * 本布局必须保住拦截能力：详情页里的 RotaryNestedScrollView 一按下就请求父级不拦截，
     * 照常传上去的话，从详情页往左滑永远到不了评论页（26.10.21 真机踩过）。
     * 竖直滚动不会被误伤——只有「横向位移 > 2 倍竖向」时本布局才接管。
     */
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        super.requestDisallowInterceptTouchEvent(false)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                lastX = ev.x
                startItem = pagerProvider?.invoke()?.currentItem ?: 0
                horizontal = false
                decided = false
                multiTouch = false
                fakeDragging = false
                blockHorizontal = touchesHorizontalControl(ev.x, ev.y)
                // 上一次「滑出退出」的动画可能还没结束，复位避免带着位移开始新手势
                animate().cancel()
                translationX = 0f
                return false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // 双指（缩放）不参与翻页
                multiTouch = true
                return horizontal
            }

            MotionEvent.ACTION_MOVE -> {
                if (multiTouch) return false
                // 拖进度条/横向滚动控件时一律不接管
                if (blockHorizontal) return false
                if (decided) return horizontal

                val dx = ev.x - downX
                val dy = ev.y - downY
                if (abs(dx) < touchSlop && abs(dy) < touchSlop) return false

                decided = true
                // 横向必须明显占优：60° 以内的斜滑都算竖向，交给子 View
                horizontal = abs(dx) > abs(dy) * 2
                if (horizontal) {
                    lastX = ev.x
                    fakeDragging = pagerProvider?.invoke()?.beginFakeDrag() == true
                }
                return horizontal
            }

            else -> {}
        }
        return horizontal
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!horizontal) return super.onTouchEvent(ev)
                if (fakeDragging) {
                    val delta = lastX - ev.x
                    pagerProvider?.invoke()?.fakeDragBy(delta)
                    // 第 0 页向右滑时 pager 无页可退，用整页位移给出「要退出」的反馈
                    if (startItem == 0 && delta < 0) {
                        translationX = (ev.x - downX).coerceAtLeast(0f)
                    }
                    lastX = ev.x
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!horizontal) return super.onTouchEvent(ev)

                val dx = ev.x - downX
                val dy = ev.y - downY
                val clearlyHorizontal = abs(dx) > abs(dy) * 2
                val passed = clearlyHorizontal && abs(dx) > width * SWIPE_MIN_RATIO
                val isUp = ev.actionMasked == MotionEvent.ACTION_UP

                if (fakeDragging) {
                    fakeDragging = false
                    val pager = pagerProvider?.invoke()
                    pager?.endFakeDrag()
                    val edgePreview = startItem == 0 && translationX > 0f
                    val current = pager?.currentItem ?: startItem
                    // 从第 0 页向右滑：fakeDrag 无页可退，吸附回原位后这里当作「退出本页」。
                    // 必须比对按下的页：否则从详情页滑回短视频首页那一下也会被误判成退出。
                    val shouldExit = isUp && passed && dx > 0 && startItem == 0 && current == 0
                    // pager 自己吸附时要求拖得足够远；我们的判定阈值更松，没吸附走就补一页，
                    // 否则「明明滑了却不翻页」会显得控件没反应
                    val snapBack = isUp && passed && current == startItem
                    when {
                        shouldExit -> {
                            // 整页滑出屏幕再关，手感与「右滑返回」一致，而不是原地突然消失
                            animate().translationX(width.toFloat()).setDuration(130).withEndAction {
                                translationX = 0f
                                onSwipe?.invoke(1)
                            }.start()
                        }

                        snapBack -> onSwipe?.invoke(if (dx < 0) -1 else 1)

                        edgePreview -> animate().translationX(0f).setDuration(130).start()
                    }
                } else if (isUp && passed) {
                    // fakeDrag 不可用时的兜底：松手按方向切页
                    onSwipe?.invoke(if (dx < 0) -1 else 1)
                }

                horizontal = false
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    /**
     * 按下点是否落在「自身要横向拖动」的控件上（底部进度条、横向滚动容器）。
     *
     * 这类控件拖到一半被父级抢走会很难用，所以整次手势都不翻页。
     */
    private fun touchesHorizontalControl(x: Float, y: Float): Boolean =
        hitHorizontalControl(this, x, y)

    private fun hitHorizontalControl(group: ViewGroup, x: Float, y: Float): Boolean {
        for (i in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue
            val cx = x + group.scrollX - child.left
            val cy = y + group.scrollY - child.top
            if (cx < 0f || cy < 0f || cx >= child.width || cy >= child.height) continue
            if (child is SeekBar || child is HorizontalScrollView) return true
            if (child is ViewGroup && hitHorizontalControl(child, cx, cy)) return true
        }
        return false
    }

    override fun onDetachedFromWindow() {
        // 页面被销毁时如果还停在 fakeDrag 状态，pager 会一直处于 DRAGGING
        if (fakeDragging) {
            fakeDragging = false
            pagerProvider?.invoke()?.endFakeDrag()
        }
        super.onDetachedFromWindow()
    }

    companion object {
        /** 横向位移要达到屏宽的这个比例才算「滑了一页」，防手抖。 */
        private const val SWIPE_MIN_RATIO = 0.18f
    }
}
