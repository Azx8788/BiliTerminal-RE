package com.RobinNotBad.BiliClient.adapter.video

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.RobinNotBad.BiliClient.R
import com.RobinNotBad.BiliClient.listener.OnItemClickListener
import com.RobinNotBad.BiliClient.model.Bangumi
import com.RobinNotBad.BiliClient.ui.appearance.ColorScheme

/**
 * 番剧详情页的选集卡片（两行：集标题 + 角标 / 长标题）。
 *
 * **不复用 [MediaEpisodeAdapter]**：那个是单行 MaterialButton 样式，直播页的清晰度、线路
 * 也在用它（语义是"选一个线路"），番剧这一集要展示长标题、会员/限免角标和"正在播放"标记，
 * 两者需求不同，混在一起会让直播页跟着变形。
 */
class EpisodeCardAdapter : RecyclerView.Adapter<EpisodeCardAdapter.EpisodeHolder>() {

    private var episodeList: List<Bangumi.Episode>? = null

    var listener: OnItemClickListener? = null

    /** 当前选中（正在播）的那一集，切季/定位后由调用方赋值。 */
    var selectedItemIndex: Int = 0
        set(value) {
            val previous = field
            field = value
            notifyItemChanged(previous)
            notifyItemChanged(value)
        }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(episodeList: List<Bangumi.Episode>) {
        this.episodeList = episodeList
        selectedItemIndex = 0
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cell_episode_card, parent, false)
        return EpisodeHolder(view)
    }

    override fun onBindViewHolder(holder: EpisodeHolder, position: Int) {
        if (position < 0 || episodeList == null || position >= episodeList!!.size) return
        holder.listener = listener
        holder.bind(position, position == selectedItemIndex)
    }

    override fun getItemCount(): Int = episodeList?.size ?: 0

    /** 选集角标的三类语义，颜色在 [EpisodeHolder.badgeColor] 里映射。 */
    enum class BadgeKind { NONE, VIP, FREE, OTHER }

    companion object {
        /**
         * 把接口给的角标文案归类（纯函数，便于单测）。
         *
         * 服务端角标不是稳定枚举而是展示文案（"会员"、"限免"、有时还有"独家"之类），
         * 所以这里只能按关键字判断；认不出来的一律归 [BadgeKind.OTHER]，照旧显示但不抢眼。
         */
        fun badgeKind(badgeText: String?): BadgeKind = when {
            badgeText.isNullOrEmpty() -> BadgeKind.NONE
            badgeText.contains("会员") -> BadgeKind.VIP
            badgeText.contains("限免") -> BadgeKind.FREE
            else -> BadgeKind.OTHER
        }
    }

    inner class EpisodeHolder(view: View) : RecyclerView.ViewHolder(view) {

        var listener: OnItemClickListener? = null
        private val playing: ImageView = itemView.findViewById(R.id.episode_playing)
        private val title: TextView = itemView.findViewById(R.id.episode_title)
        private val badge: TextView = itemView.findViewById(R.id.episode_badge)
        private val titleLong: TextView = itemView.findViewById(R.id.episode_title_long)

        fun bind(currentIndex: Int, isSelected: Boolean) {
            val episode = episodeList?.getOrNull(currentIndex) ?: return
            val context = itemView.context
            val textColor =
                if (isSelected) ColorScheme.getPrimary(context) else ColorScheme.getTextPrimary(context)

            title.text = episode.title
            title.setTextColor(textColor)

            // "正在播放"标记只在当前这一集出现，它同时也是选中态的第一眼信号
            playing.visibility = if (isSelected) View.VISIBLE else View.GONE
            if (isSelected) playing.setColorFilter(ColorScheme.getPrimary(context))

            val badgeText = episode.badge
            when (badgeKind(badgeText)) {
                BadgeKind.NONE -> badge.visibility = View.GONE
                else -> {
                    badge.visibility = View.VISIBLE
                    badge.text = badgeText
                    badge.setTextColor(badgeColor(context, badgeKind(badgeText)))
                }
            }

            val longText = episode.title_long
            if (longText.isNullOrEmpty()) {
                titleLong.visibility = View.GONE
            } else {
                titleLong.visibility = View.VISIBLE
                titleLong.text = longText
                titleLong.setTextColor(textColor)
            }

            itemView.setOnClickListener {
                selectedItemIndex = currentIndex
                listener?.onItemClick(currentIndex)
            }
        }

        /** 会员/限免角标给专门的颜色，其余角标降级成次要文字色，避免和集标题抢注意力。 */
        private fun badgeColor(context: Context, kind: BadgeKind): Int = when (kind) {
            BadgeKind.VIP -> ColorScheme.getVipPink(context)
            BadgeKind.FREE -> ColorScheme.getSuccessColor(context)
            else -> ColorScheme.getTextTertiary(context)
        }
    }
}
