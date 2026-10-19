package com.RobinNotBad.BiliClient.activity.user.info

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View

import androidx.annotation.NonNull

import com.RobinNotBad.BiliClient.activity.base.RefreshListFragment
import com.RobinNotBad.BiliClient.adapter.video.UserVideoAdapter
import com.RobinNotBad.BiliClient.api.UserInfoApi
import com.RobinNotBad.BiliClient.model.VideoCard
import com.RobinNotBad.BiliClient.util.CenterThreadPool

class UserVideoFragment : RefreshListFragment() {

    private var mid: Long = 0
    private var videoList: ArrayList<VideoCard> = ArrayList()
    private var adapter: UserVideoAdapter? = null

    companion object {
        fun newInstance(mid: Long): UserVideoFragment {
            val fragment = UserVideoFragment()
            val args = Bundle()
            args.putLong("mid", mid)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mid = arguments!!.getLong("mid")
        }
    }

    override fun onViewCreated(@NonNull view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoList = ArrayList()
        setOnLoadMoreListener { page -> continueLoading(page) }

        CenterThreadPool.run {
            try {
                bottom = (UserInfoApi.getUserVideos(mid, page, "", videoList) == 1)
                if (isAdded) {
                    setRefreshing(false)
                    val newAdapter = UserVideoAdapter(requireContext(), mid, videoList)
                    adapter = newAdapter
                    setAdapter(newAdapter)
                    if (bottom && videoList.isEmpty()) showEmptyView()
                }
            } catch (e: Exception) {
                // 首屏失败时 adapter 会一直是 null（本页没有重试入口），
                // 后续翻页回调必须容忍 adapter 为空，见 continueLoading()
                loadFail(e)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun continueLoading(page: Int) {
        CenterThreadPool.run {
            try {
                val list: MutableList<VideoCard> = ArrayList()
                val result = UserInfoApi.getUserVideos(mid, page, "", list)
                if (result != -1) {
                    Log.e("debug", "下一页")
                    runOnUiThread {
                        // 数据先累加（adapter 持有的就是 videoList 这个实例）
                        videoList.addAll(list)
                        // 首屏加载失败时 adapter 从未被赋值；view 已销毁时也不该再碰 UI。
                        // 26.10.16 线上 NPE(#43–#49) 即此处 adapter!! 在 adapter 为 null 时抛出的无 message NPE。
                        if (isAdded && view != null) {
                            // UserVideoAdapter 第 0 项是「视频系列」入口（getItemCount() = size + 1），
                            // 所以插入起点是 sizeBefore + 1；漏掉 +1 会让 RecyclerView 算错插入位置。
                            adapter?.notifyItemRangeInserted(
                                videoList.size - list.size + 1,
                                list.size
                            )
                        }
                    }
                    if (result == 1) {
                        Log.e("debug", "到底了")
                        bottom = true
                    }
                }
            } catch (e: Exception) {
                loadFail(e)
            } finally {
                // 成功、失败、以及上面 UI 回调里的提前放弃都不能卡住刷新态
                setRefreshing(false)
            }
        }
    }
}