package com.RobinNotBad.BiliClient.activity.user.info

import com.RobinNotBad.BiliClient.util.UiText

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View

import androidx.annotation.NonNull

import com.RobinNotBad.BiliClient.activity.base.RefreshListFragment
import com.RobinNotBad.BiliClient.adapter.dynamic.DynamicHolder
import com.RobinNotBad.BiliClient.adapter.dynamic.UserDynamicAdapter
import com.RobinNotBad.BiliClient.api.DynamicApi
import com.RobinNotBad.BiliClient.api.UserInfoApi
import com.RobinNotBad.BiliClient.model.Dynamic
import com.RobinNotBad.BiliClient.model.UserInfo
import com.RobinNotBad.BiliClient.util.CenterThreadPool
import com.RobinNotBad.BiliClient.util.MsgUtil

class UserDynamicFragment : RefreshListFragment() {

    private var mid: Long = 0
    private var dynamicList: ArrayList<Dynamic> = ArrayList()
    private var adapter: UserDynamicAdapter? = null
    private var offset: Long = 0

    companion object {
        fun newInstance(mid: Long): UserDynamicFragment {
            val fragment = UserDynamicFragment()
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

        dynamicList = ArrayList()
        setOnLoadMoreListener { continueLoading() }

        CenterThreadPool.run {
            try {
                val userInfo = UserInfoApi.getUserInfo(mid)
                if (userInfo == null) {
                    runOnUiThread {
                        MsgUtil.showMsg(UiText.of("用户不存在"))
                        // 这里用 activity? 而不是 requireActivity()：回调是 post 到主线程的，
                        // 真正执行时 Fragment 可能已经 onDetach，requireActivity() 会抛 ISE
                        activity?.finish()
                    }
                    return@run
                }
                Log.e("debug", "获取到用户信息")

                try {
                    offset = DynamicApi.getDynamicList(dynamicList, offset, mid, null)
                    bottom = (offset == -1L)
                    Log.e("debug", "获取到用户动态")
                } catch (e: Exception) {
                    loadFail(e)
                }

                if (isAdded) {
                    val newAdapter = UserDynamicAdapter(requireContext(), dynamicList, userInfo)
                    adapter = newAdapter
                    setAdapter(newAdapter)
                    setRefreshing(false)
                }
            } catch (e: Exception) {
                // 首屏失败时 adapter 会一直是 null（本页没有重试入口），
                // 后续翻页回调必须容忍 adapter 为空，见 continueLoading()
                loadFail(e)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun continueLoading() {
        CenterThreadPool.run {
            try {
                val list: MutableList<Dynamic> = ArrayList()
                offset = DynamicApi.getDynamicList(list, offset, mid, null)
                runOnUiThread {
                    // 数据在主线程累加：adapter 持有的就是 dynamicList 这一个实例，
                    // 所以即使下面不通知 UI，数据也不会丢。
                    dynamicList.addAll(list)
                    // 26.10.16 线上 NPE(#43–#49，无 message) 就出在这句 adapter!! 上：
                    // 首屏加载失败时 onViewCreated 的 catch 只调了 loadFail()（已把刷新态复位）、
                    // adapter 从未被赋值，用户再拖动列表就会触发翻页，回调里 adapter!! 直接抛 NPE。
                    // 另外 view 已销毁时也不要再碰 UI（防止对已解绑的 RecyclerView 发通知）。
                    if (isAdded && view != null) {
                        adapter?.notifyItemRangeInserted(
                            dynamicList.size - list.size + 1,
                            list.size
                        )
                    }
                }
                bottom = (offset == -1L)
            } catch (e: Exception) {
                loadFail(e)
            } finally {
                // 成功、失败、以及上面 UI 回调里的提前放弃都不能卡住刷新态，
                // 否则下拉/翻页会永久转圈（协议：加载完必须 setRefreshing(false)）
                setRefreshing(false)
            }
        }
    }

    fun onDynamicRemove(position: Int) {
        try {
            // 首屏失败时 adapter 可能为 null；位置也可能是旧的（动态已被删除）→ 保持原有吞异常行为
            adapter?.let { DynamicHolder.removeDynamicFromList(dynamicList, position, it) }
        } catch (ignored: Throwable) {
        }
    }
}