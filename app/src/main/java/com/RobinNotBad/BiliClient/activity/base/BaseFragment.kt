package com.RobinNotBad.BiliClient.activity.base

import android.content.Context
import androidx.fragment.app.Fragment
import com.RobinNotBad.BiliClient.BiliTerminal

open class BaseFragment : Fragment() {
    /**
     * 在 UI 线程跑任务。
     *
     * 判活**必须写在 Runnable 内部**：`CenterThreadPool.runOnUiThread` 是无条件 post（主线程调用
     * 也不内联），在外面判 `isAdded` 拿到的是投递时刻的状态，等 Runnable 真正执行时 fragment 可能
     * 已经 detach（用户返回、宿主 Activity finish、被系统回收），此时 Runnable 里再碰
     * `getString()` / `requireContext()` 就会抛
     * `IllegalStateException: Fragment ... not attached to a context.`，而且是抛在主线程 Handler 上
     * ——直接崩 App。线上崩溃 #62 就是这个形态。
     *
     * 另外 `view == null` 也要判：fragment 进回退栈时 view 已销毁但 fragment 仍是 added，
     * 这时候再去操作 setUp 出来的 view 没有意义。
     */
    fun runOnUiThread(runnable: Runnable) {
        val host = activity ?: return
        host.runOnUiThread {
            if (isAdded && view != null) runnable.run()
        }
    }

    fun getAppContext(): Context {
        return BiliTerminal.context!!
    }
}
