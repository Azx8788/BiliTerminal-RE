package com.RobinNotBad.BiliClient.player

import org.json.JSONArray
import org.json.JSONException

/**
 * 「跳过赞助片段」的纯判定逻辑（无 Android 依赖，可直接 JVM 单测）。
 *
 * 数据来源是 bsbsb.top（BilibiliSponsorBlock 的公共端口）的 `/api/skipSegments`，
 * 返回数组里每项形如：
 * ```
 * {"segment":[12.3,45.6],"UUID":"...","category":"sponsor","videoDuration":600,
 *  "actionType":"skip","locked":0,"votes":3,"description":"","cid":"123456"}
 * ```
 * 与 [ViewPointSkip] 的分工一致：本文件只回答「该不该跳、跳到哪」，真正的 seek 由播放页完成。
 */
object SponsorBlockSkip {

    /** 单段允许自动跳过的最大时长（秒）：超过视为脏数据，宁可漏跳也不把用户甩到片尾。 */
    const val MAX_SEGMENT_SECONDS = 900.0

    /** 短于该时长的片段不跳：SponsorBlock 里这种多半是误标。 */
    const val MIN_SEGMENT_SECONDS = 1.0

    /** 一条可跳过的片段。 */
    data class Segment(
        val uuid: String,
        val category: String,
        val fromSec: Double,
        val toSec: Double,
        val cid: String,
        val votes: Int
    )

    /** 片段的稳定标识：UUID 优先，缺失时退回「类别@起-止」。 */
    fun keyOf(segment: Segment): String =
        if (segment.uuid.isNotEmpty()) segment.uuid
        else segment.category + "@" + segment.fromSec + "-" + segment.toSec

    /**
     * 从接口原始 JSON 里筛出可跳过的片段，按起点升序。
     *
     * 丢弃：`actionType` 不是 `skip`（mute/full 等）、投票为负（被社区踩下去）、
     * 区间反向或为空、短于 [MIN_SEGMENT_SECONDS]、超过 [MAX_SEGMENT_SECONDS]、
     * 类别不在 [enabledCategories] 里，以及 cid 与当前分P 不一致的条目。
     *
     * 为什么要按 cid 过滤：多分P 视频的所有片段都在同一个 videoID 下返回，服务端不保证
     * 按 cid 收窄（实测带 cid 参数时返回结果不减少），不自己过滤就会把第 3 P 的广告
     * 跳段套到第 1 P 上。
     *
     * @param raw 接口原始响应（JSON 数组字符串），非法时返回空表
     * @param enabledCategories 允许跳过的类别；空集合表示不跳
     * @param currentCid 当前分P 的 cid（字符串形式）；为空则不做 cid 过滤
     */
    fun buildSegments(
        raw: String,
        enabledCategories: Set<String>,
        currentCid: String?
    ): List<Segment> {
        if (raw.isBlank() || enabledCategories.isEmpty()) return emptyList()
        val array = try {
            JSONArray(raw)
        } catch (_: JSONException) {
            return emptyList()
        }
        val result = ArrayList<Segment>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optString("actionType", "skip") != "skip") continue
            if (obj.optInt("votes", 0) < 0) continue
            val category = obj.optString("category", "")
            if (!enabledCategories.contains(category)) continue
            val pair = obj.optJSONArray("segment") ?: continue
            if (pair.length() < 2) continue
            val from = pair.optDouble(0, Double.NaN)
            val to = pair.optDouble(1, Double.NaN)
            if (from.isNaN() || to.isNaN()) continue
            if (to <= from) continue
            if (to - from < MIN_SEGMENT_SECONDS) continue
            if (to - from > MAX_SEGMENT_SECONDS) continue
            val cid = obj.optString("cid", "")
            if (currentCid != null && cid.isNotEmpty() && cid != currentCid) continue
            result.add(
                Segment(
                    uuid = obj.optString("UUID", ""),
                    category = category,
                    fromSec = from,
                    toSec = to,
                    cid = cid,
                    votes = obj.optInt("votes", 0)
                )
            )
        }
        return result.sortedBy { it.fromSec }
    }

    /** [positionSec] 落在哪个片段里。区间**左闭右开**，理由同 [ViewPointSkip.segmentAt]。 */
    fun segmentAt(positionSec: Double, segments: List<Segment>): Segment? {
        if (positionSec.isNaN() || positionSec < 0) return null
        return segments.firstOrNull { positionSec >= it.fromSec && positionSec < it.toSec }
    }

    /**
     * 当前位置应当跳过的**链**（首尾相接或互相重叠的若干段算一条链）；不需要跳时返回 null。
     *
     * 为什么成链而不是逐段跳：两段刚好相接时（A=[100,200]、B=[200,300]），只跳到 200 的
     * 下一帧又会命中 B，用户看到的是进度条连跳两次；重叠的两段还会因为第一次 seek 后落点
     * 在第二段中央，被 [keyForManualSeek] 误判成「用户手动拖入」。这里一次算到链尾，只 seek 一次。
     *
     * @param handled 本次播放已处理过的片段 key
     */
    fun skipChainAt(
        positionSec: Double,
        segments: List<Segment>,
        handled: Set<String>
    ): List<Segment>? {
        if (positionSec.isNaN() || positionSec < 0) return null
        val ordered = segments.sortedBy { it.fromSec }
        val chain = ArrayList<Segment>(2)
        var cursor = positionSec
        while (true) {
            val segment = ordered.firstOrNull {
                !handled.contains(keyOf(it)) && !chain.contains(it) &&
                    cursor >= it.fromSec && cursor < it.toSec
            } ?: break
            chain.add(segment)
            cursor = segment.toSec
        }
        return chain.ifEmpty { null }
    }

    /**
     * 链的落点（秒）。链尾把所有片段甩在身后，不会触发自身的左闭右开判定。
     *
     * 视频时长已知且链尾越界（片段数据过期，比如视频被重新上传得更短）时，钳到时长前 0.2 秒，
     * 避免 seek 过尾直接触发播放结束。
     */
    fun targetOf(chain: List<Segment>, durationSec: Double?): Double {
        val end = chain.maxOf { it.toSec }
        return if (durationSec != null && durationSec > 0 && end > durationSec - 0.2) {
            (durationSec - 0.2).coerceAtLeast(0.0)
        } else {
            end
        }
    }

    /** 用户**手动**拖进某段时，把该段标记为已处理（否则下一轮定时器又把他弹走）。 */
    fun keyForManualSeek(
        positionSec: Double,
        segments: List<Segment>,
        handled: Set<String>
    ): String? {
        val segment = segmentAt(positionSec, segments) ?: return null
        val key = keyOf(segment)
        return if (handled.contains(key)) null else key
    }
}