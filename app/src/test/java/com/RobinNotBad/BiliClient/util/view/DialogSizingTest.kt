package com.RobinNotBad.BiliClient.util.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 弹窗尺寸预算（26.10.20「低分辨率设备弹窗显示不全」）。
 *
 * 断言里用的像素值都是 380×380 / 320×320 表盘上 `uiautomator` 实测出来的真实高度：
 * 默认密度下标题 80 / 分隔线 2 / 固定底栏 88 / 提示行 66 / 一行 88（`dialog_item_min_height`）；
 * 「界面大小」调到 1.8 倍时这些值整体 ×1.8（标题 142 / 分隔线 4 / 底栏 158 / 提示 108 / 行 158）。
 */
class DialogSizingTest {

    /** 默认密度、无「界面边距」的 380×380 手表。 */
    private val parts380 = DialogSizing.Parts(
        padding = 0, title = 80, divider = 2, buttonBar = 88, hint = 66, minScroll = 88
    )

    /** 同上，但界面被「界面大小」放大到 1.8 倍（dp 全放大，屏幕还是 380×380）。 */
    private val parts380Zoom = DialogSizing.Parts(
        padding = 0, title = 142, divider = 4, buttonBar = 158, hint = 108, minScroll = 158
    )

    private val frame380 = DialogSizing.Frame(380, 380)

    // ---------- 可用区域与宽度 ----------

    @Test
    fun 可用区域等于屏幕扣掉界面边距() {
        // 圆屏适配（横 5% / 纵 3%）= 长按屏幕设置里的那档，与 BaseActivity 同一套整数算法
        val frame = DialogSizing.available(380, 380, 5, 3)
        assertEquals(DialogSizing.Frame(342, 358), frame)
    }

    @Test
    fun 界面边距百分比越界时按上下限夹() {
        // 负值当 0；超过 30% 按 30% 算（设置页输入框的上限也是 30）
        assertEquals(DialogSizing.Frame(380, 380), DialogSizing.available(380, 380, -5, 0))
        assertEquals(DialogSizing.Frame(152, 380), DialogSizing.available(380, 380, 200, 0))
    }

    @Test
    fun 弹窗宽度是可用宽度的九成() {
        assertEquals(342, DialogSizing.width(frame380))
        // 窗口永远不比可用宽度宽，再窄的屏也至少留 1px
        assertEquals(9, DialogSizing.width(DialogSizing.Frame(10, 10)))
        assertEquals(1, DialogSizing.width(DialogSizing.Frame(1, 1)))
    }

    @Test
    fun 高度预算是可用高度的九成四() {
        // 380 上留 23px：上下各约 11px 能点到「窗口外」（点空白取消靠它）
        assertEquals(357, DialogSizing.heightBudget(frame380))
        assertEquals(300, DialogSizing.heightBudget(DialogSizing.Frame(320, 320)))
        // 预算永远不会超过可用高度，再矮的屏也至少留 1px
        assertEquals(1, DialogSizing.heightBudget(DialogSizing.Frame(1, 1)))
    }

    // ---------- 正常屏幕：不改动 ----------

    @Test
    fun 放得下时一条都不舍也不撑高() {
        // 隐私说明正文自然高度 140px：80 + 2 + 140 + 88 + 66 = 376 ≤ 380
        val plan = DialogSizing.plan(frame380, natural = 140, parts = parts380)
        assertEquals(342, plan.width)
        assertFalse(plan.hideHint)
        assertFalse(plan.hideTitle)
        // null = wrap_content：以前固定撑到屏高 45%（171px），两三行正文下方留一大片空白
        assertNull(plan.scrollHeight)
    }

    @Test
    fun 长正文在正常屏幕上保留提示行并滚动() {
        // 236 + 88 ≤ 380，给正文留得出的滚动区够大 → 提示行留着（它正是用来告诉用户能滚）
        val plan = DialogSizing.plan(frame380, natural = 500, parts = parts380)
        assertFalse(plan.hideHint)
        assertFalse(plan.hideTitle)
        assertEquals(380 - 236, plan.scrollHeight)
    }

    @Test
    fun 手机宽屏不被撑到整屏宽() {
        val parts = DialogSizing.Parts(
            padding = 0, title = 120, divider = 3, buttonBar = 132, hint = 59, minScroll = 132
        )
        val plan = DialogSizing.plan(DialogSizing.Frame(1080, 1920), natural = 224, parts = parts)
        assertEquals(972, plan.width)
        assertNull(plan.scrollHeight)
    }

    // ---------- 低分辨率：本次修的正是这里 ----------

    @Test
    fun 三百二十的表盘舍掉提示行就能全放下() {
        // 320 上：236 + 122 > 320，可舍掉 66px 的提示行后 292 ≤ 320
        val plan = DialogSizing.plan(DialogSizing.Frame(320, 320), natural = 122, parts = parts380)
        assertEquals(288, plan.width)
        assertTrue(plan.hideHint)
        assertFalse(plan.hideTitle)
        assertNull(plan.scrollHeight)
    }

    @Test
    fun 三百的表盘同样靠舍提示行收口() {
        // 300 上：236 + 122 > 300，舍掉提示行后 292 ≤ 300（旧实现要 353px，底部按钮与提示行都落到屏外）
        val plan = DialogSizing.plan(DialogSizing.Frame(300, 300), natural = 122, parts = parts380)
        assertTrue(plan.hideHint)
        assertNull(plan.scrollHeight)
    }

    @Test
    fun 二百四十的表盘连标题一起舍() {
        // 240 上：连 170 都放不下 122+170=292 > 240 → 只剩底栏 88 时 122+88=210 ≤ 240
        val plan = DialogSizing.plan(DialogSizing.Frame(240, 240), natural = 122, parts = parts380)
        assertTrue(plan.hideHint)
        assertTrue(plan.hideTitle)
        assertNull(plan.scrollHeight)
    }

    @Test
    fun 界面放大一八倍时只保底栏和可滚动的正文() {
        // 380×380 放大 1.8 倍：仅标题+底栏+提示行就 412 > 380，旧实现把底栏挤成 63px、提示行整个消失
        val plan = DialogSizing.plan(frame380, natural = 400, parts = parts380Zoom)
        assertTrue(plan.hideHint)
        assertTrue(plan.hideTitle)
        assertEquals(380 - 158, plan.scrollHeight)
    }

    @Test
    fun 滚动区高度不会被算成负数() {
        // 连底栏都比屏幕高（界面放大到离谱）：只能给 0，不能给负值（会把布局撑乱）
        val parts = DialogSizing.Parts(
            padding = 0, title = 200, divider = 0, buttonBar = 400, hint = 0, minScroll = 200
        )
        val plan = DialogSizing.plan(DialogSizing.Frame(320, 320), natural = 50, parts = parts)
        assertEquals(0, plan.scrollHeight)
    }

    @Test
    fun 方案里的宽度永远等于宽度函数的结果() {
        val frames = listOf(
            DialogSizing.Frame(380, 380),
            DialogSizing.Frame(320, 320),
            DialogSizing.Frame(1080, 1920),
            DialogSizing.Frame(1, 1)
        )
        frames.forEach { frame ->
            assertEquals(DialogSizing.width(frame), DialogSizing.plan(frame, 0, parts380).width)
        }
    }

    // ---------- 加上高度预算后：整体一定收在屏内 ----------

    @Test
    fun 三百八的表盘按高度预算收口且不超屏() {
        // 380×380 真实跑下来的那一帧：正文自然高 600 > 能给的 121，弹窗整体 357 ≤ 380
        val budget = DialogSizing.Frame(380, DialogSizing.heightBudget(frame380))
        val plan = DialogSizing.plan(budget, natural = 600, parts = parts380)
        assertFalse(plan.hideHint)
        assertFalse(plan.hideTitle)
        assertEquals(357 - 236, plan.scrollHeight)
        // 固定行 + 滚动区 = 预算高度，不会把底栏/提示行挤到屏外
        assertEquals(budget.height, 236 + plan.scrollHeight!!)
    }

    @Test
    fun 界面放大一八倍时加上预算也不超屏() {
        // 380×380 放大 1.8 倍：标题 + 底栏 + 提示行 412 已经比 357 的预算还高，
        // 全让位后只剩底栏 158 + 正文 199 = 357，底栏永远在屏内
        val budget = DialogSizing.Frame(380, DialogSizing.heightBudget(frame380))
        val plan = DialogSizing.plan(budget, natural = 1573, parts = parts380Zoom)
        assertTrue(plan.hideHint)
        assertTrue(plan.hideTitle)
        assertEquals(357 - 158, plan.scrollHeight)
    }
}
