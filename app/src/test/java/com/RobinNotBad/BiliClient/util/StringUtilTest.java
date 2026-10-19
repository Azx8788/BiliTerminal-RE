package com.RobinNotBad.BiliClient.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
}
