package com.RobinNotBad.BiliClient.player

import com.RobinNotBad.BiliClient.player.LocalAudioTrackPolicy.EmbeddedAudio
import com.RobinNotBad.BiliClient.player.LocalAudioTrackPolicy.Selection
import com.RobinNotBad.BiliClient.player.LocalAudioTrackPolicy.TrackKind
import org.junit.Assert.assertEquals
import org.junit.Test

/** 测试轨道真相与选择，不把 Android/IJK 桩当作实际解码测试。 */
class LocalAudioTrackPolicyTest {
    @Test
    fun 含音轨的MP4加残留M4A只播放主文件() {
        assertEquals(Selection.MAIN_ONLY, select(TrackKind.VIDEO, TrackKind.AUDIO))
    }

    @Test
    fun DASH纯视频加M4A启用外部音频() {
        assertEquals(Selection.EXTERNAL, select(TrackKind.VIDEO))
    }

    @Test
    fun 仅音频主文件不重复播放第二路() {
        assertEquals(Selection.MAIN_ONLY, select(TrackKind.AUDIO))
    }

    @Test
    fun 多个内嵌音轨也不能再启动外部音频() {
        assertEquals(Selection.MAIN_ONLY,
            select(TrackKind.VIDEO, TrackKind.AUDIO, TrackKind.AUDIO))
    }

    @Test
    fun 轨道顺序不影响内嵌音轨优先级() {
        assertEquals(Selection.MAIN_ONLY,
            select(TrackKind.OTHER, TrackKind.AUDIO, TrackKind.VIDEO))
    }

    @Test
    fun 字幕等其他轨道不被错认成音频() {
        assertEquals(Selection.EXTERNAL, select(TrackKind.OTHER, TrackKind.VIDEO))
    }

    @Test
    fun 元数据读取失败保持未知而非无音轨() {
        assertEquals(EmbeddedAudio.UNKNOWN, LocalAudioTrackPolicy.embeddedAudio(null))
        assertEquals(Selection.UNKNOWN,
            LocalAudioTrackPolicy.select(true, true, EmbeddedAudio.UNKNOWN))
    }

    @Test
    fun 空轨道列表不能证明无音轨() {
        assertEquals(EmbeddedAudio.UNKNOWN, LocalAudioTrackPolicy.embeddedAudio(emptyList()))
        assertEquals(Selection.UNKNOWN, select())
    }

    @Test
    fun 只有未知或字幕轨道时不盲目启动外部音频() {
        assertEquals(Selection.UNKNOWN, select(TrackKind.OTHER))
    }

    @Test
    fun 外部文件缺失空或不可读时不启动() {
        for (audio in EmbeddedAudio.entries) {
            assertEquals(Selection.MAIN_ONLY, LocalAudioTrackPolicy.select(true, false, audio))
        }
    }

    @Test
    fun 在线或直播主文件不能复用本地旧音轨() {
        for (audio in EmbeddedAudio.entries) {
            assertEquals(Selection.MAIN_ONLY, LocalAudioTrackPolicy.select(false, true, audio))
        }
    }

    @Test
    fun 重载媒体后重新判断而不是沿用旧结果() {
        assertEquals(Selection.EXTERNAL, select(TrackKind.VIDEO))
        assertEquals(Selection.MAIN_ONLY, select(TrackKind.VIDEO, TrackKind.AUDIO))
        assertEquals(Selection.EXTERNAL, select(TrackKind.VIDEO))
    }

    private fun select(vararg tracks: TrackKind): Selection = LocalAudioTrackPolicy.select(
        mainIsLocal = true,
        externalFileUsable = true,
        embeddedAudio = LocalAudioTrackPolicy.embeddedAudio(tracks.toList())
    )
}
