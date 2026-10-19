package com.RobinNotBad.BiliClient.activity.user

import com.RobinNotBad.BiliClient.util.UiText

import android.os.Bundle
import android.widget.EditText
import android.widget.RadioGroup
import com.RobinNotBad.BiliClient.R
import com.RobinNotBad.BiliClient.activity.base.BaseActivity
import com.RobinNotBad.BiliClient.api.UserInfoApi
import com.RobinNotBad.BiliClient.util.CenterThreadPool
import com.RobinNotBad.BiliClient.util.MsgUtil
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil
import com.google.android.material.card.MaterialCardView

class EditUserInfoActivity : BaseActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etBirthday: EditText
    private lateinit var rgSex: RadioGroup
    private lateinit var submit: MaterialCardView
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user_info)

        if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0L) {
            MsgUtil.showMsg(UiText.of("还没有登录喵~"))
            finish()
            return
        }

        etUsername = findViewById(R.id.et_username)
        etBirthday = findViewById(R.id.et_birthday)
        rgSex = findViewById(R.id.rg_sex)
        submit = findViewById(R.id.submit)

        findViewById<android.view.View>(R.id.pageName).setOnClickListener { finish() }

        CenterThreadPool.run {
            try {
                val userInfo = UserInfoApi.getCurrentUserInfo()
                if (userInfo != null && userInfo.name.isNotEmpty()) {
                    runOnUiThread { etUsername.setText(userInfo.name) }
                }
            } catch (e: Exception) {
                // 预填失败不影响提交
            }
        }

        submit.setOnClickListener {
            if (isSubmitting) {
                MsgUtil.showMsg(UiText.of("正在提交中..."))
                return@setOnClickListener
            }

            if (!SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.cookie_refresh, true)) {
                MsgUtil.showDialog(getString(R.string.text_cannot_submit), getString(R.string.msg_cookie_refresh_failed), -1)
                return@setOnClickListener
            }

            val uname = etUsername.text.toString().trim()
            val birthday = etBirthday.text.toString().trim()
            if (birthday.isNotEmpty() && !birthday.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                MsgUtil.showMsg(UiText.of("生日格式错误，请使用YYYY-MM-DD"))
                return@setOnClickListener
            }

            val sex = when (rgSex.checkedRadioButtonId) {
                R.id.rb_male -> "1"
                R.id.rb_female -> "2"
                else -> null
            }

            if (uname.isEmpty() && birthday.isEmpty() && sex == null) {
                MsgUtil.showMsg(UiText.of("请至少填写一项要修改的内容"))
                return@setOnClickListener
            }

            isSubmitting = true
            submit.isEnabled = false

            CenterThreadPool.run {
                try {
                    val result = UserInfoApi.updateUserInfo(
                        if (uname.isEmpty()) null else uname,
                        if (birthday.isEmpty()) null else birthday,
                        sex,
                        null
                    )
                    val code = result.getInt("code")
                    val message = result.optString("message", "")

                    if (!isDestroyed) {
                        runOnUiThread {
                            isSubmitting = false
                            submit.isEnabled = true

                            if (code == 0) {
                                MsgUtil.showMsg(UiText.of("修改成功"))
                                finish()
                            } else {
                                val errorMsg = when (code) {
                                    -101 -> UiText.of("账号未登录")
                                    -111 -> UiText.of("CSRF验证失败")
                                    400 -> UiText.of("昵称违规或已被占用")
                                    412 -> UiText.of("修改频率过高，请稍后再试")
                                    2001 -> UiText.of("昵称已存在")
                                    21003 -> UiText.of("生日格式错误")
                                    -403 -> UiText.of("权限不足")
                                    else -> if (message.isNotEmpty()) message else "修改失败"
                                }
                                MsgUtil.showMsg(errorMsg)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!isDestroyed) {
                        runOnUiThread {
                            isSubmitting = false
                            submit.isEnabled = true
                            MsgUtil.err(UiText.of("修改失败"), e)
                        }
                    }
                }
            }
        }
    }
}
