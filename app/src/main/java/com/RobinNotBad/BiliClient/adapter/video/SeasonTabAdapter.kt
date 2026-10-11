package com.RobinNotBad.BiliClient.adapter.video

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.RobinNotBad.BiliClient.R
import com.RobinNotBad.BiliClient.model.Bangumi
import com.RobinNotBad.BiliClient.ui.appearance.ColorScheme

/**
 * 番剧详情页「选集」区的季 tab 行（横向滚动）。
 *
 * 数据源是 `info.seasons`（同系列所有季），与官方客户端的选集 tab 同源；
 * 交互对齐 PiliPlus 的 EpisodePanel：**tab 数 > 1 才显示整行**（显示与否由页面控制），
 * 选中 tab 用主题主色 + 加粗，未选中用次级文字色（不画底色，省得和卡片背景打架）。
 *
 * 点击已选中的 tab 不发回调：切季要发网络请求，没必要为一次无效点击走一遍。
 */
class SeasonTabAdapter(
    private val seasonList: List<Bangumi.Season>
) : RecyclerView.Adapter<SeasonTabAdapter.SeasonTabHolder>() {

    fun interface OnTabClickListener {
        fun onTabClick(index: Int)
    }

    /** 切换选中项；只刷新前后两项（这条行通常只有 2~4 项，逐项刷新也不贵） */
    var selectedIndex: Int = 0
        set(value) {
            val previous = field
            field = value
            if (previous != value) {
                notifyItemChanged(previous)
                notifyItemChanged(value)
            }
        }

    private var listener: OnTabClickListener? = null

    fun setOnTabClickListener(listener: OnTabClickListener) {
        this.listener = listener
    }

    fun getItem(index: Int): Bangumi.Season? =
        if (index in seasonList.indices) seasonList[index] else null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonTabHolder =
        SeasonTabHolder(LayoutInflater.from(parent.context).inflate(R.layout.cell_season_tab, parent, false))

    override fun onBindViewHolder(holder: SeasonTabHolder, position: Int) {
        if (position !in seasonList.indices) return
        val season = seasonList[position]

        holder.tabText.text = season.season_title
        val selected = position == selectedIndex
        val context = holder.itemView.context
        holder.tabText.setTextColor(
            if (selected) ColorScheme.getPrimary(context) else ColorScheme.getTextSecondary(context)
        )
        holder.tabText.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        holder.tabText.setOnClickListener {
            if (position != selectedIndex) listener?.onTabClick(position)
        }
    }

    override fun getItemCount(): Int = seasonList.size

    class SeasonTabHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tabText: TextView = itemView.findViewById(R.id.tab_text)
    }
}
