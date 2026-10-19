package com.RobinNotBad.BiliClient.activity.user.info

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View

import androidx.annotation.NonNull

import com.RobinNotBad.BiliClient.activity.base.RefreshListFragment
import com.RobinNotBad.BiliClient.adapter.article.ArticleCardAdapter
import com.RobinNotBad.BiliClient.api.UserInfoApi
import com.RobinNotBad.BiliClient.model.ArticleCard
import com.RobinNotBad.BiliClient.util.CenterThreadPool

class UserArticleFragment : RefreshListFragment() {

    private var mid: Long = 0
    private var articleList: ArrayList<ArticleCard> = ArrayList()
    private var adapter: ArticleCardAdapter? = null

    companion object {
        fun newInstance(mid: Long): UserArticleFragment {
            val fragment = UserArticleFragment()
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

        articleList = ArrayList()
        setOnLoadMoreListener { page -> continueLoading(page) }

        CenterThreadPool.run {
            try {
                bottom = (UserInfoApi.getUserArticles(mid, page, articleList) == 1)
                if (isAdded) {
                    val newAdapter = ArticleCardAdapter(requireContext(), articleList)
                    adapter = newAdapter
                    setAdapter(newAdapter)
                    setRefreshing(false)
                    if (bottom && articleList.isEmpty()) showEmptyView()
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
                val list: MutableList<ArticleCard> = ArrayList()
                val result = UserInfoApi.getUserArticles(mid, page, list)
                if (result != -1) {
                    Log.e("debug", "下一页")
                    runOnUiThread {
                        // 数据先累加（adapter 持有的就是 articleList 这个实例）
                        articleList.addAll(list)
                        // 首屏加载失败时 adapter 从未被赋值；view 已销毁时也不该再碰 UI。
                        // 26.10.16 线上 NPE(#43–#49) 即此处 adapter!! 在 adapter 为 null 时抛出的无 message NPE。
                        if (isAdded && view != null) {
                            // ArticleCardAdapter 无头部，插入起点就是 sizeBefore
                            adapter?.notifyItemRangeInserted(
                                articleList.size - list.size,
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