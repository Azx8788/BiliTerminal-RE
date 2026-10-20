package com.RobinNotBad.BiliClient.activity.player

/**
 * 普通播放器双击的纯判据。方向不是缩放状态，不能参与复位或播放/暂停的判断。
 * 缩放取画面的实际比例，不依赖可能过期的手势监听器标志（判据在 [PlayerZoomPolicy]）。
 */
object PlayerDoubleTapPolicy {
    enum class Action { RESET_SCALE, TOGGLE_PLAYBACK, IGNORE }

    fun resolve(
        restoreScaleFirst: Boolean,
        scaleEnabled: Boolean,
        scaleX: Float,
        scaleY: Float,
        isLive: Boolean
    ): Action {
        val hasScale = PlayerZoomPolicy.isZoomed(scaleX, scaleY)
        return when {
            restoreScaleFirst && scaleEnabled && hasScale -> Action.RESET_SCALE
            !isLive -> Action.TOGGLE_PLAYBACK
            else -> Action.IGNORE
        }
    }
}
