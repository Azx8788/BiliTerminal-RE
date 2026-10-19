package com.RobinNotBad.BiliClient.activity.user

import com.RobinNotBad.BiliClient.util.UiText

import android.os.Bundle
import android.view.View

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.RobinNotBad.BiliClient.R
import com.RobinNotBad.BiliClient.activity.base.BaseActivity
import com.RobinNotBad.BiliClient.adapter.LogListAdapter
import com.RobinNotBad.BiliClient.api.ExpLogApi
import com.RobinNotBad.BiliClient.model.ExpLog
import com.RobinNotBad.BiliClient.util.CenterThreadPool
import com.RobinNotBad.BiliClient.util.MsgUtil

class ExpLogActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var logList: ArrayList<ExpLog> = arrayListOf()
    private var adapter: LogListAdapter<ExpLog>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_refresh)

        setPageName(UiText.of("经验变化记录"))

        recyclerView = findViewById(R.id.recyclerView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        swipeRefreshLayout.isEnabled = false
        swipeRefreshLayout.isRefreshing = true

        recyclerView.layoutManager = LinearLayoutManager(this)

        logList = ArrayList()

        CenterThreadPool.run {
            try {
                logList = ArrayList(ExpLogApi.getExpLog())

                runOnUiThread {
                    if (logList.isEmpty()) {
                        MsgUtil.showMsg(UiText.of("暂无经验变化记录"))
                        findViewById<View>(R.id.emptyTip).visibility = View.VISIBLE
                    } else {
                        // 经验流水：保持原有行为，恒前置 "+"
                        adapter = LogListAdapter(this, logList) { log ->
                            LogListAdapter.Row("+" + log.delta, log.reason, log.time)
                        }
                        recyclerView.adapter = adapter
                    }
                    swipeRefreshLayout.isRefreshing = false
                }
            } catch (e: Exception) {
                runOnUiThread {
                    MsgUtil.showMsg(UiText.fmt("加载失败：%1\$s", e.message))
                    swipeRefreshLayout.isRefreshing = false
                    findViewById<View>(R.id.emptyTip).visibility = View.VISIBLE
                }
                e.printStackTrace()
            }
        }
    }
}