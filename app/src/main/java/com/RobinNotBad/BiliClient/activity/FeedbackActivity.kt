package com.RobinNotBad.BiliClient.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import com.RobinNotBad.BiliClient.R
import com.RobinNotBad.BiliClient.activity.base.BaseActivity
import com.RobinNotBad.BiliClient.api.TerminalApi
import com.RobinNotBad.BiliClient.util.CenterThreadPool
import com.RobinNotBad.BiliClient.util.LogExporter
import com.RobinNotBad.BiliClient.util.Logu
import com.RobinNotBad.BiliClient.util.MsgUtil
import com.RobinNotBad.BiliClient.util.SettingsKeys
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil
import com.RobinNotBad.BiliClient.util.TerminalDialog
import com.google.android.material.button.MaterialButton

/**
 * 反馈与建议（26.10.09 新增）。
 *
 * <p>这是**本项目自建的一条反馈通道**，与上游原有的 QQ 群 / issue 渠道并存，不替换任何东西：
 * 内容直接 POST 到 [TerminalApi.BASE_URL]（自建服务器，见 `server/rebiliterminal_api.py`），
 * 走的是不带 Cookie 的自建请求头，所以**不需要登录**也能发。
 *
 * <h3>隐私边界</h3>
 * 只发三类东西：用户自己写的内容、用户选的联系方式（可空）、以及版本号与机型。
 * 账号 ID 默认**不带**，只有用户在本页（或设置页）显式打开「附带我的账号 ID」才会带上；
 * 这个开关的状态存在 [SettingsKeys.FEEDBACK_ATTACH_MID]，默认 false。
 * 应用日志同样默认**不附带**：只有用户在本页勾选「附带应用日志」才会把
 * [Logu.ringSnapshot] 的**内存日志快照**（已脱敏）塞进 `log` 字段——反馈是用户主动行为，
 * 由他决定带不带（与崩溃页的默认勾选相反）。本页另有「导出日志到文件」入口，走
 * [LogExporter]，只把日志留在本机、不经过网络。
 * 改这里的字段之前先去看 `TerminalApi.buildFeedbackPayload`，两边的字段表必须一致。
 *
 * <h3>为什么不用 ViewModel / Retrofit</h3>
 * 这是工程既有约定（`api/` 全是 static + `org.json`，没有 DI）。请求与解析已经拆开：
 * 解析在 [TerminalApi] 的纯函数里，有 JVM 单测覆盖；本类只负责收集输入与渲染状态。
 */
class FeedbackActivity : BaseActivity() {

    companion object {
        /** 与服务端 `CONTENT_MAX` 保持一致。 */
        private const val MAX_CONTENT = 4000
    }

    /** 当前选中的分类（wire 值，不是显示名）。 */
    private var category: String = TerminalApi.CATEGORY_BUG

    /** 是否附带账号 ID。默认读设置；本页改动会立刻写回设置，两个入口共用一份状态。 */
    private var attachMid: Boolean = false

    /** 发送中。挡住连点造成的重复提交（服务端也有每小时 10 次的限流兜底）。 */
    private var sending: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        asyncInflate(R.layout.activity_feedback) { _, _ ->
            setPageName(getString(R.string.pagename_feedback))

            val categoryText = findViewById<TextView>(R.id.category_text)
            val categoryCard = findViewById<View>(R.id.category_card)
            val contentEdit = findViewById<EditText>(R.id.content_edit)
            val contentCount = findViewById<TextView>(R.id.content_count)
            val contactEdit = findViewById<EditText>(R.id.contact_edit)
            val attachCard = findViewById<View>(R.id.attach_mid_card)
            val attachText = findViewById<TextView>(R.id.attach_mid_text)
            val attachLogCheck = findViewById<CheckBox>(R.id.attach_log_check)
            val exportLogCard = findViewById<View>(R.id.export_log_card)
            val submitBtn = findViewById<MaterialButton>(R.id.submit_btn)
            val statusView = findViewById<TextView>(R.id.submit_status)

            // ---------- 分类 ----------
            categoryText.text = labelOf(category)
            categoryCard.setOnClickListener {
                val items = TerminalApi.CATEGORIES.map { labelOf(it) }
                val checked = TerminalApi.CATEGORIES.indexOf(category).coerceAtLeast(0)
                TerminalDialog.singleChoice(
                    this, getString(R.string.feedback_category_label), items, checked
                ) { sheet, index ->
                    category = TerminalApi.CATEGORIES[index]
                    categoryText.text = labelOf(category)
                    sheet.dialog.dismiss()
                }.show()
            }

            // ---------- 字数 ----------
            contentEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    contentCount.text = "${s?.length ?: 0} / $MAX_CONTENT"
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            contentCount.text = "0 / $MAX_CONTENT"

            // ---------- 附带账号 ID ----------
            attachMid = SharedPreferencesUtil.getBoolean(SettingsKeys.FEEDBACK_ATTACH_MID, false)

            fun renderAttach() {
                attachText.text = getString(
                    if (attachMid) R.string.feedback_attach_mid_on else R.string.feedback_attach_mid_off
                )
            }

            renderAttach()
            attachCard.setOnClickListener {
                attachMid = !attachMid
                SharedPreferencesUtil.putBoolean(SettingsKeys.FEEDBACK_ATTACH_MID, attachMid)
                renderAttach()
            }

            // ---------- 附带应用日志 ----------
            // 默认不勾选，且**不持久化**：每次进页面都是关的，避免上次的选择被悄悄沿用
            // （与「附带账号 ID」不同——那个是长期偏好，这个是一次性决定）。
            attachLogCheck.isChecked = false

            // ---------- 导出日志到文件 ----------
            // 与提交是两条独立路径：这里只落本机文件并调起系统分享，不经过网络。
            exportLogCard.setOnClickListener { LogExporter.export(this) }

            // ---------- 发送 ----------
            submitBtn.setOnClickListener {
                if (sending) return@setOnClickListener

                val content = contentEdit.text?.toString()?.trim().orEmpty()
                if (content.isEmpty()) {
                    MsgUtil.showMsg(getString(R.string.feedback_empty))
                    return@setOnClickListener
                }
                if (content.length > MAX_CONTENT) {
                    MsgUtil.showMsg(getString(R.string.feedback_too_long))
                    return@setOnClickListener
                }
                val contact = contactEdit.text?.toString()?.trim().orEmpty()
                // 在主线程读一次勾选状态，带进子线程用（View 不跨线程访问）
                val attachLog = attachLogCheck.isChecked

                sending = true
                submitBtn.isEnabled = false
                statusView.visibility = View.VISIBLE
                statusView.text = getString(R.string.feedback_sending)

                CenterThreadPool.run {
                    // 关掉开关时传 0：服务端那条记录里根本不会有 mid 这个字段值，
                    // 不是「传了但不显示」——隐私上这两者的区别很重要。
                    val mid = if (attachMid) SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0L) else 0L
                    // 勾选才取内存日志快照；拼接最长 64K 字符，放在子线程避免任何主线程开销。
                    val log = if (attachLog) Logu.ringSnapshot() else ""
                    val result = TerminalApi.submitFeedback(category, content, log, contact, mid)

                    runOnUiThread {
                        sending = false
                        if (isDestroyed) return@runOnUiThread
                        submitBtn.isEnabled = true
                        if (result.code >= 0) {
                            statusView.text = getString(R.string.feedback_ok, result.code)
                            // 成功才清空，失败时留着内容让用户重发，不然写的一大段就没了
                            contentEdit.setText("")
                            contactEdit.setText("")
                            // 日志已随这次反馈送达，清空内存缓冲，避免下次反馈把同一份旧日志再发一遍。
                            // 只在「确实勾选附带」且「提交成功」时清：没勾选就没送出去、失败则要留着重试。
                            // 代价是清完之后用户当场再点「导出日志」会得到空提示——可接受：缓冲是滚动的、
                            // 新日志会立刻重新累积，而想要文件留档的人本就该走导出入口（那条路径不清缓冲）。
                            if (attachLog) Logu.ringClear()
                        } else {
                            statusView.text = getString(R.string.feedback_fail, result.message)
                        }
                    }
                }
            }
        }
    }

    private fun labelOf(value: String): String = when (value) {
        TerminalApi.CATEGORY_BUG -> getString(R.string.feedback_category_bug)
        TerminalApi.CATEGORY_SUGGESTION -> getString(R.string.feedback_category_suggestion)
        TerminalApi.CATEGORY_CONTENT -> getString(R.string.feedback_category_content)
        TerminalApi.CATEGORY_PERFORMANCE -> getString(R.string.feedback_category_performance)
        else -> getString(R.string.feedback_category_other)
    }
}
