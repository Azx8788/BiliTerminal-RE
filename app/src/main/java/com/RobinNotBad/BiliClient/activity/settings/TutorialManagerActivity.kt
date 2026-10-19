package com.RobinNotBad.BiliClient.activity.settings

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.RobinNotBad.BiliClient.R
import com.RobinNotBad.BiliClient.activity.base.BaseActivity
import com.RobinNotBad.BiliClient.ui.appearance.ColorScheme
import com.RobinNotBad.BiliClient.util.MsgUtil
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class TutorialManagerActivity : BaseActivity() {

    private data class TutorialItem(
        val tag: String,
        val name: String,
        val description: String,
        val xmlArrayId: Int,
        val isPager: Boolean = false
    )

    private val tutorialItems = mutableListOf<TutorialItem>()

    @SuppressLint("MissingInflatedId", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildTutorialList()

        asyncInflate(R.layout.activity_tutorial_manager) { _, _ ->
            setPageName(getString(R.string.pagename_tutorial_manager))

            val container = findViewById<LinearLayout>(R.id.tutorial_list_container)

            for (item in tutorialItems) {
                val card = createTutorialCard(item)
                container.addView(card)
            }

            findViewById<MaterialCardView>(R.id.btn_pass_all).setOnClickListener {
                passAllTutorials()
            }

            findViewById<MaterialCardView>(R.id.btn_clear_all).setOnClickListener {
                clearAllTutorials()
            }

            findViewById<MaterialCardView>(R.id.btn_clear_pager).setOnClickListener {
                clearPagerTutorials()
            }

            findViewById<View>(R.id.scrollView).requestFocus()
        }
    }

    private fun buildTutorialList() {
        tutorialItems.clear()
        val tags = resources.getStringArray(R.array.tutorial_list)
        val names = arrayOf(
            "推荐页面教程",
            "视频详情教程",
            "用户主页教程",
            "搜索教程",
            "消息页面教程",
            "动态页教程",
            "动态详情页教程",
            "专栏详情页教程",
            "短视频教程"
        )
        val descs = arrayOf(
            "引导用户使用推荐页面和哔哩终端",
            "引导用户使用视频详情页",
            "引导用户使用用户主页",
            "引导用户使用搜索功能",
            "引导用户使用消息页面",
            "引导用户使用动态功能",
            "引导用户查看动态详情",
            "引导用户使用专栏详情页",
            "引导用户使用短视频播放功能"
        )
        val xmlArrayIds = intArrayOf(
            R.array.tutorial_recommend,
            R.array.tutorial_video,
            R.array.tutorial_space,
            R.array.tutorial_search,
            R.array.tutorial_message,
            R.array.tutorial_dynamic,
            R.array.tutorial_dynamic_info,
            R.array.tutorial_article,
            R.array.tutorial_short_video
        )

        for (i in tags.indices) {
            tutorialItems.add(
                TutorialItem(
                    tag = tags[i],
                    name = if (i < names.size) names[i] else tags[i],
                    description = if (i < descs.size) descs[i] else "",
                    xmlArrayId = if (i < xmlArrayIds.size) xmlArrayIds[i] else 0
                )
            )
        }

        // 添加页面滑动引导（显示一次后不再显示的教程）
        tutorialItems.add(TutorialItem("SearchActivity", "搜索页面引导", "搜索页面的左右翻页提示", 0, true))
        tutorialItems.add(TutorialItem("VideoInfoActivity", "视频详情页引导", "视频详情页的左右翻页提示", 0, true))
        tutorialItems.add(TutorialItem("UserInfoActivity", "用户主页引导", "用户主页的左右翻页提示", 0, true))
        tutorialItems.add(TutorialItem("OpusInfoActivity", "专栏详情页引导", "专栏详情页的左右翻页提示", 0, true))
        tutorialItems.add(TutorialItem("DynamicInfoActivity", "动态详情页引导", "动态详情页的左右翻页提示", 0, true))
    }

    private fun isTutorialCompleted(tag: String): Boolean {
        return SharedPreferencesUtil.getInt("tutorial_ver_$tag", -1) >= 0
    }

    private fun hasTutorialHistory(tag: String): Boolean {
        return SharedPreferencesUtil.getBoolean("tutorial_history_$tag", false)
    }

    private fun getTutorialVersion(tag: String): Int {
        return SharedPreferencesUtil.getInt("tutorial_ver_$tag", -1)
    }

    private fun isPagerTutorialCompleted(tag: String): Boolean {
        return !SharedPreferencesUtil.getBoolean("tutorial_pager_$tag", true)
    }

    private fun createTutorialCard(item: TutorialItem): MaterialCardView {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            radius = resources.getDimension(R.dimen.card_round)
            isClickable = false
            isFocusable = false
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(
                dp2px(8f),
                dp2px(8f),
                dp2px(8f),
                dp2px(8f)
            )
        }

        // 颜色一律走主题色或既有语义色资源，不写死色值：写死白色在浅色主题下看不见标题，
        // 写死绿/橙也不跟主题走（依据 AGENTS.md「颜色只读 ?attr/ 或既有 color 资源」）。
        val textOnColor = ContextCompat.getColor(this, R.color.text_on_primary)
        val successColor = ContextCompat.getColor(this, R.color.status_success)
        val warningColor = ContextCompat.getColor(this, R.color.status_warning)

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = item.name
            textSize = 13f
            setTextColor(ColorScheme.TEXT_PRIMARY)
        }

        val statusBadge = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(dp2px(8f), dp2px(2f), dp2px(8f), dp2px(2f))
        }

        val isCompleted = if (item.isPager) isPagerTutorialCompleted(item.tag) else isTutorialCompleted(item.tag)

        if (isCompleted) {
            statusBadge.text = getString(R.string.text_tutorial_done)
            statusBadge.setTextColor(textOnColor)
            // View.backgroundTintList 的 setter 是 API 21 才有的，minSdk 19 上要走 ViewCompat；
            // API < 21 时它等价于 no-op（statusBadge 本身没设 background），不会崩。
            ViewCompat.setBackgroundTintList(statusBadge, ColorStateList.valueOf(successColor))
        } else {
            statusBadge.text = getString(R.string.text_tutorial_undone)
            statusBadge.setTextColor(textOnColor)
            ViewCompat.setBackgroundTintList(statusBadge, ColorStateList.valueOf(warningColor))
        }

        titleRow.addView(titleText)
        titleRow.addView(statusBadge)

        val descText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp2px(4f)
            }
            text = item.description
            textSize = 11f
            setTextColor(ColorScheme.TEXT_SECONDARY)
        }

        val historyText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp2px(2f)
            }
            textSize = 10f
            visibility = View.GONE
        }

        if (!item.isPager && hasTutorialHistory(item.tag)) {
            historyText.text = getString(R.string.text_tutorial_history_done, getTutorialVersion(item.tag))
            historyText.setTextColor(ColorScheme.TEXT_TERTIARY)
            historyText.visibility = View.VISIBLE
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp2px(6f)
            }
        }

        val passBtn = MaterialButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                dp2px(32f),
                1f
            ).apply {
                marginEnd = dp2px(4f)
            }
            text = "通过教程"
            textSize = 11f
            isAllCaps = false
            setOnClickListener {
                passTutorial(item)
            }
        }

        val clearBtn = MaterialButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                dp2px(32f),
                1f
            ).apply {
                marginStart = dp2px(4f)
            }
            text = "清除进度"
            textSize = 11f
            isAllCaps = false
            setOnClickListener {
                clearSingleTutorial(item)
            }
        }

        buttonRow.addView(passBtn)
        buttonRow.addView(clearBtn)

        container.addView(titleRow)
        container.addView(descText)
        container.addView(historyText)
        container.addView(buttonRow)

        card.addView(container)

        return card
    }

    // ============ 通过教程 ============

    private fun passTutorial(item: TutorialItem) {
        if (item.isPager) {
            SharedPreferencesUtil.putBoolean("tutorial_pager_${item.tag}", false)
        } else {
            val xmlArray = resources.getStringArray(item.xmlArrayId)
            if (xmlArray.isNotEmpty()) {
                SharedPreferencesUtil.putBoolean("tutorial_history_${item.tag}", true)
                SharedPreferencesUtil.putInt("tutorial_ver_${item.tag}", xmlArray.size)
            }
        }
        MsgUtil.showMsg(getString(R.string.msg_tutorial_passed, item.name))
        recreate()
    }

    // ============ 清除单个教程进度 ============

    private fun clearSingleTutorial(item: TutorialItem) {
        if (item.isPager) {
            SharedPreferencesUtil.removeValue("tutorial_pager_${item.tag}")
        } else {
            SharedPreferencesUtil.removeValue("tutorial_ver_${item.tag}")
            SharedPreferencesUtil.removeValue("tutorial_history_${item.tag}")
        }
        MsgUtil.showMsg(getString(R.string.msg_tutorial_progress_cleared, item.name))
        recreate()
    }

    // ============ 一键通过所有 ============

    private fun passAllTutorials() {
        for (item in tutorialItems) {
            if (item.isPager) {
                SharedPreferencesUtil.putBoolean("tutorial_pager_${item.tag}", false)
            } else {
                val xmlArray = resources.getStringArray(item.xmlArrayId)
                if (xmlArray.isNotEmpty()) {
                    SharedPreferencesUtil.putBoolean("tutorial_history_${item.tag}", true)
                    SharedPreferencesUtil.putInt("tutorial_ver_${item.tag}", xmlArray.size)
                }
            }
        }
        MsgUtil.showMsg(getString(R.string.msg_tutorialmanageractivity_001))
        recreate()
    }

    // ============ 清除所有教程进度 ============

    private fun clearAllTutorials() {
        for (item in tutorialItems) {
            if (item.isPager) {
                SharedPreferencesUtil.removeValue("tutorial_pager_${item.tag}")
            } else {
                SharedPreferencesUtil.removeValue("tutorial_ver_${item.tag}")
                SharedPreferencesUtil.removeValue("tutorial_history_${item.tag}")
            }
        }
        MsgUtil.showMsg(getString(R.string.msg_tutorialmanageractivity_002))
        recreate()
    }

    // ============ 重置页面滑动引导 ============

    private fun clearPagerTutorials() {
        val pagerKeys = arrayOf(
            "SearchActivity", "VideoInfoActivity", "UserInfoActivity",
            "OpusInfoActivity", "DynamicInfoActivity"
        )
        for (key in pagerKeys) {
            SharedPreferencesUtil.removeValue("tutorial_pager_$key")
        }
        MsgUtil.showMsg(getString(R.string.msg_tutorialmanageractivity_003))
        recreate()
    }

    private fun dp2px(dp: Float): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }
}