package com.RobinNotBad.BiliClient.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 行高与字号折算的纯函数测试。
 *
 * <p>为什么不再直接调 {@code getTextHeightWithSize(null)}：纯 JVM 下 {@code android.graphics.Paint}
 * 是桩，构造即抛 "not mocked"（{@code app/build.gradle} 未开启 {@code returnDefaultValues}），
 * 原用例 catch 住 RuntimeException 后只断言「不是 NPE / 不是 Error」，恒成立、等于没断言（审计 M13-d）。
 * 现在把不依赖框架的公式抽成纯函数分别断言具体数值；取 FontMetrics 的那一段依赖框架类，
 * 纯 JVM 里拿不到，只能在真机上验证。
 */
public class StringUtilTest {

    @Test
    public void textHeightSizePx_scalesWithDensity() {
        // 12sp 基准字号 × 密度；context 为 null 与非 null 两条分支只是密度来源不同，折算公式共用这一处。
        assertEquals(12f, StringUtil.textHeightSizePx(1.0f), 0.001f);
        assertEquals(24f, StringUtil.textHeightSizePx(2.0f), 0.001f);
        assertEquals(42f, StringUtil.textHeightSizePx(3.5f), 0.001f);
    }

    @Test
    public void textHeightSizePx_zeroDensity_isZero() {
        // 密度异常（0）时不能折出一个正数，否则 setBounds 会画出一条不该有的行高
        assertEquals(0f, StringUtil.textHeightSizePx(0f), 0.001f);
    }

    @Test
    public void lineHeightOf_isDescentMinusAscent() {
        // ascent 为负、descent 为正；相减才是字形实际占高（写反就变成负数或 0）
        assertEquals(13f, StringUtil.lineHeightOf(-3f, 10f), 0.001f);
        assertEquals(13f, StringUtil.lineHeightOf(-10.5f, 2.5f), 0.001f);
        assertTrue("ascent 为负时行高必须为正", StringUtil.lineHeightOf(-12f, 4f) > 0);
    }

    @Test
    public void isLongPressGesture_trueOnlyWhenHeldPastThreshold() {
        int timeout = 500;
        // 按住 600ms：长按回调已经跑过，不能再补一次链接点击
        assertTrue(StringUtil.isLongPressGesture(true, 1000L, 1600L, timeout));
        // 按住 200ms：普通点击，必须点开链接
        assertFalse(StringUtil.isLongPressGesture(true, 1000L, 1200L, timeout));
    }

    @Test
    public void isLongPressGesture_boundaryCountsAsLongPress() {
        // 恰好等于阈值按长按处理：View 在阈值时刻已经触发了 OnLongClickListener
        assertTrue(StringUtil.isLongPressGesture(true, 1000L, 1500L, 500));
        assertFalse(StringUtil.isLongPressGesture(true, 1000L, 1499L, 500));
    }

    @Test
    public void isLongPressGesture_notLongClickableNeverBlocksLinkClick() {
        // 没有长按监听器时长按回调根本不会发生，按住再久抬手也应该点开链接（否则链接点不动）
        assertFalse(StringUtil.isLongPressGesture(false, 1000L, 9000L, 500));
    }

    @Test
    public void isLongPressGesture_missingDownTimeKeepsOldBehavior() {
        // 没记录到按下时间（从别处滑进链接区域等）：按点击处理，保持旧行为
        assertFalse(StringUtil.isLongPressGesture(true, -1L, 9000L, 500));
    }
}
