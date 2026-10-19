package com.RobinNotBad.BiliClient.activity.user

import com.RobinNotBad.BiliClient.util.UiText

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

import com.RobinNotBad.BiliClient.R
import com.RobinNotBad.BiliClient.activity.base.BaseActivity
import com.RobinNotBad.BiliClient.api.VipApi
import com.RobinNotBad.BiliClient.model.VipInfo
import com.RobinNotBad.BiliClient.util.CenterThreadPool
import com.RobinNotBad.BiliClient.util.MsgUtil
import com.RobinNotBad.BiliClient.util.StringUtil
import com.RobinNotBad.BiliClient.util.TimeUtil

import org.json.JSONException
import org.json.JSONObject

import java.io.IOException

class VipActivity : BaseActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asyncInflate(R.layout.activity_vip) { _, _ ->
            CenterThreadPool.run {
                try {
                    val vipInfo = VipApi.getVipInfo()

                    runOnUiThread {
                        try {
                            val vipStatusText = findViewById<TextView>(R.id.vipStatus)
                            val vipTypeText = findViewById<TextView>(R.id.vipType)
                            val vipDueDateText = findViewById<TextView>(R.id.vipDueDate)
                            val levelText = findViewById<TextView>(R.id.level)
                            val expText = findViewById<TextView>(R.id.exp)
                            val bindPhoneText = findViewById<TextView>(R.id.bindPhone)

                            if (vipInfo.isVip) {
                                vipStatusText.text = UiText.of("是")
                                if (vipInfo.vipIsAnnual) {
                                    vipTypeText.text = UiText.of("年度大会员")
                                } else if (vipInfo.vipIsMonth) {
                                    vipTypeText.text = UiText.of("月大会员")
                                } else {
                                    vipTypeText.text = UiText.of("大会员")
                                }
                                if (vipInfo.vipDueDate > 0) {
                                    vipDueDateText.text = TimeUtil.formatDateTimeSec(vipInfo.vipDueDate * 1000)
                                } else {
                                    vipDueDateText.text = UiText.of("未知")
                                }
                            } else {
                                vipStatusText.text = UiText.of("否")
                                vipTypeText.text = UiText.of("无")
                                vipDueDateText.text = UiText.of("无")
                            }

                            levelText.text = vipInfo.level.toString()
                            if (vipInfo.nextExp == -1L) {
                                expText.text = StringUtil.toWan(vipInfo.curExp) + UiText.of(" (已满级)")
                            } else {
                                expText.text = StringUtil.toWan(vipInfo.curExp) + " / " + StringUtil.toWan(vipInfo.nextExp)
                            }

                            if (vipInfo.bindPhone.isNotEmpty()) {
                                bindPhoneText.text = vipInfo.bindPhone
                            } else {
                                bindPhoneText.text = UiText.of("未绑定")
                            }

                            if (vipInfo.privilegeList != null && vipInfo.privilegeList.isNotEmpty()) {
                                val privilegeSection = findViewById<View>(R.id.privilegeSection)
                                privilegeSection.visibility = View.VISIBLE
                                val privilegeListText = findViewById<TextView>(R.id.privilegeList)
                                val privilegeBuilder = StringBuilder()
                                val privilegeNames = arrayOf(UiText.of("B币兑换"), "会员购优惠券", "漫画福利券", "会员购包邮券",
                                    UiText.of("漫画商城优惠券"), UiText.of("装扮体验卡"), UiText.of("课堂优惠券"), UiText.of("游戏礼盒"), UiText.of("每日10经验"))
                                for (privilege in vipInfo.privilegeList) {
                                    if (privilege.type >= 1 && privilege.type <= 9) {
                                        val name = privilegeNames[privilege.type - 1]
                                        val state: String
                                        if (privilege.state == 0) {
                                            state = UiText.of("未兑换")
                                        } else if (privilege.state == 1) {
                                            state = UiText.of("已兑换")
                                        } else {
                                            state = UiText.of("未完成")
                                        }
                                        privilegeBuilder.append(name).append(": ").append(state).append("\n")
                                    }
                                }
                                if (privilegeBuilder.length > 0) {
                                    privilegeListText.text = privilegeBuilder.toString().trim { it <= ' ' }
                                }
                            }

                            val experienceButton = findViewById<Button>(R.id.experienceButton)

                            experienceButton.setOnClickListener {
                                CenterThreadPool.run {
                                    try {
                                        val result = VipApi.addExperience()
                                        val code = result.getInt("code")
                                        val message = result.optString("message", "")
                                        val data = result.optJSONObject("data")
                                        runOnUiThread {
                                            if (code == 0 && data != null && data.optBoolean("is_grant", false)) {
                                                MsgUtil.showMsg(UiText.of("领取成功"))
                                            } else if (code == 69198) {
                                                MsgUtil.showMsg(UiText.of("今日已领取"))
                                            } else {
                                                MsgUtil.showMsg("领取失败: $message")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        runOnUiThread { MsgUtil.err(e) }
                                    }
                                }
                            }

                            val scrollView = findViewById<View>(R.id.scrollView)
                            scrollView.isFocusable = true
                            scrollView.isFocusableInTouchMode = true
                            scrollView.requestFocus()
                        } catch (e: Exception) {
                            runOnUiThread { MsgUtil.err(e) }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { MsgUtil.err(e) }
                }
            }
        }
    }
}