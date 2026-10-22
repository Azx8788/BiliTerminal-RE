package com.RobinNotBad.BiliClient.player

/**
 * 「跳过赞助片段」的设置默认值与类别（纯逻辑，无 Android 依赖，可直接 JVM 单测）。
 *
 * 与 [SkipOpEdPrefs] 同一套约定：
 * - [DEFAULT_ENABLED] 只决定「键不存在时读什么」，老用户的显式存档优先；
 * - [FEATURE_ENABLED] 是与存档无关的总闸门，出问题时可一键下线，且不需要回滚判定逻辑。
 */
object SponsorBlockPrefs {

    /** `player_skip_sponsor` 的默认值：开启。跳过有「撤回」兜底，误跳代价可控。 */
    const val DEFAULT_ENABLED = true

    /** 功能总开关：与用户存档无关的否决位，供紧急下线用。 */
    const val FEATURE_ENABLED = true

    /**
     * 默认跳过的类别：赞助商广告（sponsor）与创作者自我推广（selfpromo）。
     *
     * 其余类别（互动提醒 interaction、片尾 outro、预告 preview 等）各人偏好差异大，
     * 首版不替用户决定，需要时再扩设置项。
     */
    val DEFAULT_CATEGORIES: Set<String> = setOf("sponsor", "selfpromo")

    /** 自动跳过当前是否生效：功能总开关 **与** 用户存档，两者都为真才算开。 */
    fun isEnabled(prefValue: Boolean): Boolean = FEATURE_ENABLED && prefValue
}