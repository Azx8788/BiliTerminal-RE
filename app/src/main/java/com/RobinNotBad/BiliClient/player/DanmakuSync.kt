package com.RobinNotBad.BiliClient.player

/**
 * 弹幕时钟纠偏的纯判定逻辑（无 Android 依赖，可直接 JVM 单测）。
 *
 * ## 为什么不是「偏差超过 400ms 就 seek」
 *
 * DFM 的 `seekTo` **不是免费的**：`DrawHandler.seekTo` → `DrawTask.seek()` → `reset()`，
 * 会把屏上全部弹幕清空（`danmakus = new Danmakus()` + `mRenderer.clear()`），
 * 再靠 `CacheManagingDrawTask` 的 `SEEK` 分支 `prepareCaches(true)` 重建缓存
 * （见 `DanmakuFlameMaster/.../controller/DrawTask.java` 的 `seek()` 与
 * `CacheManagingDrawTask.java` 的 `CacheHandler.SEEK`）。
 *
 * 于是「小偏差也纠偏」在用户眼里就是：**弹幕整屏消失 → 过零点几秒重新出现 → 再消失**。
 * 只要漂移持续存在（缓冲/卡顿时弹幕时钟按墙钟走、播放位置却停在原地），每秒一次 seek
 * 就等于每秒清一次屏——这正是 26.10.21 用户报的「弹幕突然消失、拖进度条后闪一下就没了」。
 *
 * ## 现在的策略
 *
 * 1. **只吸附肉眼可见的大跳**（[SNAP_THRESHOLD_MS] = 1.5s）。1.5s 以内的偏差不碰，
 *    因为清屏的代价远大于偏差本身。
 * 2. **吸附后给冷静期**（[SETTLE_MS]），期间不再判定，让缓存重建与计时基准稳定下来。
 * 3. **连续吸附仍不收敛就退避**（[MAX_CONSECUTIVE_SNAPS] 次后 [BACKOFF_MS]），
 *    避免「判定为真 → seek → 清零重建 → 又被判定为真」的死循环。
 * 4. 一旦某次采样落在阈值内（收敛），连续计数立刻清零，恢复正常灵敏度。
 */
object DanmakuSync {

    /** 播放位置的采样间隔（`PlayerActivity.progressChange` 的 250ms 自循环）。 */
    const val SAMPLE_INTERVAL_MS = 250L

    /**
     * 只有偏差**超过**这个值才吸附。
     *
     * 必须远大于采样滞后（250ms）：位置是每 250ms 采样一次，天然落后 0~250ms，
     * 若在这个量级就判定，会每次采样都把 DFM 自走的时钟拽一次（滚动弹幕"一跳一跳"）。
     */
    const val SNAP_THRESHOLD_MS = 1500L

    /** 吸附后的冷静期：这段时间内即使仍有偏差也不再吸附（给重建缓存留时间）。 */
    const val SETTLE_MS = 1500L

    /** 连续吸附这么多次后判定「吸附治不了」，转入退避。 */
    const val MAX_CONSECUTIVE_SNAPS = 3

    /** 退避时长。期间弹幕时钟按 DFM 自走的基准跑，不再清屏。 */
    const val BACKOFF_MS = 30_000L

    /**
     * 一次采样的判定结果。
     *
     * @property snap 是否应当 `IDanmakuView.seekTo(posMs)`
     * @property consecutive 更新后的「连续吸附次数」，调用方保存并在下次采样传回
     */
    data class Decision(val snap: Boolean, val consecutive: Int)

    /**
     * 判定这一次采样要不要吸附弹幕时钟。
     *
     * @param posMs 当前播放位置（毫秒）。**负数表示位置不可信**（播放器未就绪或正在重建），
     *   此时绝不吸附——把窗口期的脏位置灌进去会让整批弹幕被判成"已过期"而一条都不显示。
     * @param timerMs DFM 当前的弹幕时钟（`IDanmakuView.currentTime`）。
     * @param nowMs 本次采样的时间（`SystemClock.elapsedRealtime()`）。
     * @param lastSnapAtMs 上次吸附的时间；从未吸附过传一个足够小的值（如 0）。
     * @param consecutive 上次返回的连续吸附次数。
     */
    fun decide(
        posMs: Long,
        timerMs: Long,
        nowMs: Long,
        lastSnapAtMs: Long,
        consecutive: Int
    ): Decision {
        if (posMs < 0) return Decision(false, consecutive)

        val drift = posMs - timerMs
        if (drift in -SNAP_THRESHOLD_MS..SNAP_THRESHOLD_MS) {
            // 收敛：灵敏度立刻恢复，否则退避会一直挂着
            return Decision(false, 0)
        }
        if (nowMs - lastSnapAtMs < settleFor(consecutive)) return Decision(false, consecutive)
        return Decision(true, consecutive + 1)
    }

    /**
     * 距离上次吸附至少要等多久。
     *
     * 前 [MAX_CONSECUTIVE_SNAPS] 次只等 [SETTLE_MS]（正常的偶发大跳，例如卡顿累积）；
     * 之后说明吸附没有解决问题，退到 [BACKOFF_MS]，宁可让弹幕错位也不再反复清屏。
     */
    fun settleFor(consecutive: Int): Long =
        if (consecutive >= MAX_CONSECUTIVE_SNAPS) BACKOFF_MS else SETTLE_MS
}
