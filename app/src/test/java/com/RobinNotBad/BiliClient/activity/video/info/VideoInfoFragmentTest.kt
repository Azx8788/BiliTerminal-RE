package com.RobinNotBad.BiliClient.activity.video.info

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 进详情页上报观看记录的判据测试（26.10.15）。
 *
 * 这个判据同时决定两件互相对立的事：新视频要不要"登记"进观看记录
 * （用户实测过：点进详情页历史里什么都不多，只有真的播了才出现一条），
 * 以及多P视频的续播进度会不会被一条 0 进度抹掉。
 * 松了丢进度，紧了丢记录，两种都不好靠手点复现，只能靠单测钉住。
 */
class VideoInfoFragmentTest {

    /** 场景①：从没看过的稿件，getVideo 把 last_play_cid=0 归一化成 cidHistory=cid/progress=0。 */
    @Test
    fun 从没看过的稿件_报0进度把它登记进观看记录() {
        assertTrue(VideoInfoFragment.shouldReportHistoryOnEnter(cidHistory = 1001L, cid = 1001L))
    }

    /** 场景②：本分P有续播位置，原样回写，幂等无害。 */
    @Test
    fun 本分P有续播进度_原样回写() {
        assertTrue(VideoInfoFragment.shouldReportHistoryOnEnter(cidHistory = 1001L, cid = 1001L))
    }

    /**
     * 场景③：多P视频，上次看的是第 3P（cidHistory=3003），详情页预取的是第 1P（cid=1001）。
     * 报 (aid, 1001, 0) 会把第 3P 的续播抹成 0，用户下次打开从第 3P 开头看。
     */
    @Test
    fun 进度属于别的分P_不许报0抹掉那一P的续播() {
        assertFalse(VideoInfoFragment.shouldReportHistoryOnEnter(cidHistory = 3003L, cid = 1001L))
    }

    /** cid 缺失时无处上报（取流失败时 data.cid 可能是 0），必须挡住。 */
    @Test
    fun cid无效时不上报() {
        assertFalse(VideoInfoFragment.shouldReportHistoryOnEnter(cidHistory = 0L, cid = 0L))
    }
}
