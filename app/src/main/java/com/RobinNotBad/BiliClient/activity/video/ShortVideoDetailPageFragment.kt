package com.RobinNotBad.BiliClient.activity.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.RobinNotBad.BiliClient.R
import com.RobinNotBad.BiliClient.activity.reply.ReplyFragment
import com.RobinNotBad.BiliClient.activity.video.info.VideoInfoFragment
import com.RobinNotBad.BiliClient.activity.video.info.VideoRcmdFragment
import com.RobinNotBad.BiliClient.ui.widget.SwipePageLayout
import com.RobinNotBad.BiliClient.util.Logu

/**
 * 短视频页右侧的第 1~3 页：视频详情 / 评论 / 相关推荐。
 *
 * 为什么不直接把 [VideoInfoFragment] 等塞进外壳 pager：它们的 `newInstance(aid)` 是
 * 「创建那一刻定 aid」，而短视频会一直往下翻、当前视频随时在变。这里做成一层**壳**：
 * 壳本身按页码创建（不变），真正的子 Fragment 在 [render] 里按**当前**短视频重建。
 *
 * 换视频时不重建壳、也不让外壳 pager 重绑（见 [ShortVideoShellAdapter.refreshDetailPages]）：
 * 26.10.21 真机踩过——把 aid 编进 `getItemId` 再 `notifyItemRangeChanged`，旧 fragment 的
 * 视图被摘掉后新视图挂不回来，整页只剩背景色（日志里子 Fragment 明明 `children=1`）。
 */
class ShortVideoDetailPageFragment : Fragment() {

    private val pageKind: Int
        get() = arguments?.getInt(ARG_PAGE) ?: PAGE_DETAIL

    /** 当前锚点视频；开头来自 arguments，之后由 [updateItem] 热更新。 */
    private var anchorAid = 0L
    private var anchorBvid: String? = null

    private var container: ViewGroup? = null
    private var statusText: TextView? = null

    /** 已经塞进容器的子 Fragment 的 aid，避免重复重建。 */
    private var renderedAid = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        anchorAid = arguments?.getLong(ARG_AID) ?: 0L
        anchorBvid = arguments?.getString(ARG_BVID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_short_video_detail_page, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.detailContainer)
        statusText = view.findViewById(R.id.detailStatus)

        val host = requireActivity() as ShortVideoPlayerActivity

        // 详情三页也要能横向翻页（详情 → 评论 → 相关推荐），同样只认「明确横向」
        (view as? SwipePageLayout)?.apply {
            pagerProvider = { host.shellPager() }
            onSwipe = { direction -> host.handleSwipe(direction) }
        }

        render()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        container = null
        statusText = null
        renderedAid = 0L
    }

    /**
     * 短视频换了当前条：热更新本页内容。
     *
     * 只改数据不重建外层的壳，调用点见 [ShortVideoShellAdapter.refreshDetailPages]。
     */
    fun updateItem(aid: Long, bvid: String?) {
        if (aid == anchorAid && bvid == anchorBvid) return
        anchorAid = aid
        anchorBvid = bvid
        if (isAdded && view != null) render()
    }

    private fun render() {
        val container = container ?: return
        val statusText = statusText ?: return

        if (anchorAid <= 0L) {
            statusText.text = getString(R.string.text_shortvideoplayer_detail_not_ready)
            statusText.visibility = View.VISIBLE
            renderedAid = 0L
            return
        }
        if (renderedAid == anchorAid) return

        val target: Fragment = when (pageKind) {
            PAGE_DETAIL -> VideoInfoFragment.newInstance(anchorAid, anchorBvid)
            PAGE_REPLY -> ReplyFragment.newInstance(anchorAid, 1, 0, -1L, 0L)
            else -> VideoRcmdFragment.newInstance(anchorAid)
        }

        statusText.visibility = View.GONE
        try {
            childFragmentManager.beginTransaction()
                .replace(container.id, target)
                .commitNow()
            renderedAid = anchorAid
        } catch (e: Exception) {
            // 宿主正在销毁时 commitNow 会抛 IllegalStateException，忽略即可（下次绑定会重来）
            Logu.e("ShortVideoDetailPage", "render 失败：${e.message}")
        }
    }

    companion object {
        const val PAGE_DETAIL = 1
        const val PAGE_REPLY = 2
        const val PAGE_RELATED = 3

        private const val ARG_PAGE = "page"
        private const val ARG_AID = "aid"
        private const val ARG_BVID = "bvid"

        fun newInstance(page: Int, aid: Long, bvid: String?): ShortVideoDetailPageFragment {
            val fragment = ShortVideoDetailPageFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_PAGE, page)
                putLong(ARG_AID, aid)
                putString(ARG_BVID, bvid)
            }
            return fragment
        }
    }
}
