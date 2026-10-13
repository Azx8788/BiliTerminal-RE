package com.RobinNotBad.BiliClient.player

import com.RobinNotBad.BiliClient.activity.player.PlayerDoubleTapPolicy
import com.RobinNotBad.BiliClient.activity.player.PlayerDoubleTapPolicy.Action
import com.RobinNotBad.BiliClient.util.SettingsKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 双击策略不接收方向，避免默认方向或手动旋转抢占缩放复位与播放/暂停。 */
class PlayerDoubleTapPolicyTest {
    private fun resolve(
        restoreFirst: Boolean = true,
        scaleEnabled: Boolean = true,
        scaleX: Float = 1f,
        scaleY: Float = scaleX,
        isLive: Boolean = false
    ) = PlayerDoubleTapPolicy.resolve(restoreFirst, scaleEnabled, scaleX, scaleY, isLive)

    @Test
    fun portraitDefault_rotatedToLandscape_withoutZoomTogglesPlayback() {
        val defaultLandscape = PlayerDefaults.resolveLandscape(
            SettingsKeys.PLAYER_DEFAULT_ORIENTATION_PORTRAIT, 1920, 1080
        )
        assertFalse(defaultLandscape)
        val currentLandscape = !defaultLandscape
        assertTrue(currentLandscape)
        assertEquals(Action.TOGGLE_PLAYBACK, resolve())
    }

    @Test
    fun landscapeDefault_rotatedToPortrait_withoutZoomTogglesPlayback() {
        val defaultLandscape = PlayerDefaults.resolveLandscape(
            SettingsKeys.PLAYER_DEFAULT_ORIENTATION_LANDSCAPE, 1080, 1920
        )
        assertTrue(defaultLandscape)
        val currentLandscape = !defaultLandscape
        assertFalse(currentLandscape)
        assertEquals(Action.TOGGLE_PLAYBACK, resolve())
    }

    @Test
    fun autoOrientation_eitherResolutionWithoutZoomTogglesPlayback() {
        val auto = SettingsKeys.PLAYER_DEFAULT_ORIENTATION_AUTO
        assertTrue(PlayerDefaults.resolveLandscape(auto, 1920, 1080))
        assertFalse(PlayerDefaults.resolveLandscape(auto, 1080, 1920))
        assertEquals(Action.TOGGLE_PLAYBACK, resolve())
    }

    @Test
    fun zoomedWithPriority_resetsScale() {
        assertEquals(Action.RESET_SCALE, resolve(scaleX = 2f))
        assertEquals(Action.RESET_SCALE, resolve(scaleX = 5f))
    }

    @Test
    fun eitherAxisZoomed_needsReset() {
        assertEquals(Action.RESET_SCALE, resolve(scaleX = 2f, scaleY = 1f))
        assertEquals(Action.RESET_SCALE, resolve(scaleX = 1f, scaleY = 2f))
    }

    @Test
    fun restoredScale_nextDoubleTapTogglesPlayback() {
        assertEquals(Action.RESET_SCALE, resolve(scaleX = 2f))
        // 执行复位后两轴回到 1，下一次双击不能再吃掉暂停。
        assertEquals(Action.TOGGLE_PLAYBACK, resolve(scaleX = 1f, scaleY = 1f))
    }

    @Test
    fun priorityDisabled_evenWithZoomTogglesPlayback() {
        assertEquals(Action.TOGGLE_PLAYBACK, resolve(restoreFirst = false, scaleX = 2f))
        assertEquals(Action.TOGGLE_PLAYBACK, resolve(restoreFirst = false))
    }

    @Test
    fun scalingDisabled_doesNotConsumeDoubleTapForReset() {
        assertEquals(Action.TOGGLE_PLAYBACK, resolve(scaleEnabled = false, scaleX = 2f))
    }

    @Test
    fun tinyScaleRounding_doesNotCountAsZoom() {
        assertEquals(Action.TOGGLE_PLAYBACK, resolve(scaleX = 1.0005f, scaleY = 0.9995f))
        assertEquals(Action.RESET_SCALE, resolve(scaleX = 1.01f))
    }

    @Test
    fun liveWithZoomAndPriority_canResetButNeverTogglesPlayback() {
        assertEquals(Action.RESET_SCALE, resolve(scaleX = 2f, isLive = true))
        assertEquals(Action.IGNORE, resolve(isLive = true))
        assertEquals(Action.IGNORE, resolve(restoreFirst = false, scaleX = 2f, isLive = true))
        assertEquals(Action.IGNORE, resolve(scaleEnabled = false, scaleX = 2f, isLive = true))
    }
}
