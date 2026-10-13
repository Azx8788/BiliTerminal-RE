package com.RobinNotBad.BiliClient.player

/**
 * 本地双文件的音轨选择判据，不依赖 Android。
 * 文件存在、清晰度或扩展名都不能证明主文件没有音轨，必须以播放器解析出的轨道为准。
 * 解析失败/空轨道列表是 UNKNOWN，不能当作 ABSENT 启动第二路音频。
 */
internal object LocalAudioTrackPolicy {
    enum class TrackKind { VIDEO, AUDIO, OTHER }
    enum class EmbeddedAudio { PRESENT, ABSENT, UNKNOWN }
    enum class Selection { MAIN_ONLY, EXTERNAL, UNKNOWN }

    fun embeddedAudio(tracks: List<TrackKind>?): EmbeddedAudio = when {
        tracks == null -> EmbeddedAudio.UNKNOWN
        TrackKind.AUDIO in tracks -> EmbeddedAudio.PRESENT
        TrackKind.VIDEO in tracks -> EmbeddedAudio.ABSENT
        else -> EmbeddedAudio.UNKNOWN
    }

    fun select(mainIsLocal: Boolean, externalFileUsable: Boolean,
               embeddedAudio: EmbeddedAudio): Selection = when {
        !mainIsLocal || !externalFileUsable -> Selection.MAIN_ONLY
        embeddedAudio == EmbeddedAudio.PRESENT -> Selection.MAIN_ONLY
        embeddedAudio == EmbeddedAudio.ABSENT -> Selection.EXTERNAL
        else -> Selection.UNKNOWN
    }
}
