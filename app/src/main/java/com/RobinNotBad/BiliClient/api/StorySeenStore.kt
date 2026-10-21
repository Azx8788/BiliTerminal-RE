package com.RobinNotBad.BiliClient.api

import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil

/**
 * 「看过的短视频」短期记忆，用于**跨会话去重**。
 *
 * 背景：`StoryFeedPager` 的 `seenAids` 只活在一次进入短视频页的会话里，退出再进就丢了，
 * 服务端又时不时把刚看过的视频重新下发，表现就是「怎么老刷到同一条」。
 * 这里把最近看过的 aid 落盘，下次进页面时一并交给解析层过滤。
 *
 * 取舍：
 * - 只记**最近 [MAX_SIZE] 条**（旧记录对推荐已无意义，还会让首屏越来越难出内容）；
 * - 存成逗号分隔的字符串而不是 JSON 数组：解析无异常分支，单测可直接覆盖编解码；
 * - 只在成功拿到内容后才写，失败不影响已有记录。
 *
 * 编解码与合并都是纯函数，可直接在 JVM 单测里跑；落盘走 [SharedPreferencesUtil]。
 */
object StorySeenStore {

    private const val KEY_SEEN_AIDS = "short_video_seen_aids"

    /** 最多记住多少条；太大会让推荐池越来越窄。 */
    const val MAX_SIZE = 300

    /**
     * 合并两批 aid：保留已有顺序，追加新出现的，超出上限时从**最旧**的一端丢弃。
     *
     * @param current 当前已记录的（按时间从旧到新）
     * @param incoming 新看过的
     * @return 合并后的结果（LinkedHashSet 保持插入顺序，天然去重）
     */
    fun merge(current: Set<Long>, incoming: Collection<Long>): LinkedHashSet<Long> {
        val merged = LinkedHashSet<Long>(current.size + incoming.size)
        merged.addAll(current)
        merged.addAll(incoming)
        while (merged.size > MAX_SIZE) {
            val oldest = merged.iterator()
            oldest.next()
            oldest.remove()
        }
        return merged
    }

    /** 编码成 `aid,aid,...`；空集合返回空串。 */
    fun encode(aids: Collection<Long>): String =
        aids.joinToString(",")

    /** 解码；非法/空串一律当空集合，不抛异常。 */
    fun decode(raw: String?): LinkedHashSet<Long> {
        val result = LinkedHashSet<Long>()
        if (raw.isNullOrEmpty()) return result
        raw.split(',').forEach { part ->
            val aid = part.trim().toLongOrNull() ?: return@forEach
            if (aid > 0) result.add(aid)
        }
        return result
    }

    /** 读取已记录的 aid（按时间从旧到新）。 */
    fun load(): Set<Long> {
        return try {
            decode(SharedPreferencesUtil.getString(KEY_SEEN_AIDS, ""))
        } catch (e: Exception) {
            emptySet()
        }
    }

    /** 把新看过的 aid 并入已记录集合并落盘。 */
    fun record(seen: Collection<Long>) {
        if (seen.isEmpty()) return
        try {
            val merged = merge(load(), seen)
            SharedPreferencesUtil.putString(KEY_SEEN_AIDS, encode(merged))
        } catch (e: Exception) {
            // 记录失败不影响观看，最多下次还会重复看到同一条
        }
    }
}
