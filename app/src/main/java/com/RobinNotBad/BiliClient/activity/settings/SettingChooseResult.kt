package com.RobinNotBad.BiliClient.activity.settings

import android.app.Activity
import android.content.Intent
import com.RobinNotBad.BiliClient.adapter.SettingsAdapter
import com.RobinNotBad.BiliClient.model.SettingSection
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil

/**
 * 承接 [com.RobinNotBad.BiliClient.activity.ListChooseActivity]（list_choose 设置项）的选择结果：
 * 把选中的值写回 SharedPreferences、让对应行重新绑定，并触发该项自己的 onSelect 回调。
 *
 * <p>原先 [SettingGroupActivity] 与 [SettingTerminalPlayerActivity] 各抄了一份逐字相同的 13 行；
 * 第二份是后补的——第一份只放在分组页，于是播放器设置页里的 list_choose「点了没反应」。
 * 回写逻辑只留这一份，新页面接上 list_choose 时只需一行调用。
 *
 * @param sections 该页的设置项全表，用来把结果里的 position 反查回 section
 * @param notifyItem 通知对应位置重新绑定（各页的适配器字段名不同，由调用方传入）
 */
internal fun applyListChooseResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
    sections: List<SettingSection>,
    notifyItem: (Int) -> Unit
) {
    if (requestCode != SettingsAdapter.REQUEST_CODE_LIST_CHOOSE) return
    if (resultCode != Activity.RESULT_OK) return
    val result = data ?: return
    val position = result.getIntExtra("position", -1)
    val value = result.getStringExtra("value") ?: return
    if (position < 0 || position >= sections.size) return
    val section = sections[position]
    val oldValue = SharedPreferencesUtil.getString(section.id, section.defaultValue)
    SharedPreferencesUtil.putString(section.id, value)
    notifyItem(position)
    (section.extra as? SettingsAdapter.ListChooseHolder.ListChooseExtra)?.onSelect?.invoke(oldValue, value)
}
