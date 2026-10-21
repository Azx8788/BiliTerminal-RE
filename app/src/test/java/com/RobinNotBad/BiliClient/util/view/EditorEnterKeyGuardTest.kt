package com.RobinNotBad.BiliClient.util.view

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「回车推进焦点」崩溃的判据（线上崩溃 #66）。
 *
 * 这是纯函数层：真正的人机边界（`dispatchKeyEvent` 的 try/catch、平台抛不抛）只有在真机/模拟器上
 * 才成立，单测只钉住**该不该认这条异常**——认错方向的代价是双向的：漏认还会崩，错认会把
 * 真正的 ISE 藏起来。
 */
class EditorEnterKeyGuardTest {

    private val platformMessage = "focus search returned a view that wasn't able to take focus!"

    @Test
    fun 平台原文的_ISE_应被认出() {
        assertTrue(EditorEnterKeyGuard.isFocusSearchFailure(IllegalStateException(platformMessage)))
    }

    @Test
    fun 包在_cause_链里的平台异常也要认出() {
        // 线上真实形态：ExitCatch / 上报链路可能再包一层
        val outer = RuntimeException("wrap", IllegalStateException(platformMessage))
        assertTrue(EditorEnterKeyGuard.isFocusSearchFailure(outer))
    }

    @Test
    fun 消息不逐字相同的_ISE_不算() {
        // 大小写 / 空白 / 省略号 都是不同消息；平台这条是常量，不做模糊匹配
        assertFalse(EditorEnterKeyGuard.isFocusSearchFailure(IllegalStateException(platformMessage.uppercase())))
        assertFalse(EditorEnterKeyGuard.isFocusSearchFailure(IllegalStateException(" $platformMessage")))
        assertFalse(EditorEnterKeyGuard.isFocusSearchFailure(IllegalStateException(platformMessage.dropLast(1))))
    }

    @Test
    fun 别的_ISE_不算() {
        assertFalse(EditorEnterKeyGuard.isFocusSearchFailure(IllegalStateException("Fragment not attached to a context.")))
        assertFalse(EditorEnterKeyGuard.isFocusSearchFailure(IllegalStateException()))
    }

    @Test
    fun 同类消息但不是_ISE_不算() {
        // 只有 IllegalStateException 才是平台那条路径；同样文案的 RuntimeException 不该被吞
        assertFalse(EditorEnterKeyGuard.isFocusSearchFailure(RuntimeException(platformMessage)))
    }

    @Test
    fun 空异常不算() {
        assertFalse(EditorEnterKeyGuard.isFocusSearchFailure(null))
    }

    @Test
    fun 超深_cause_链按层数上限放弃() {
        // 防止被循环/恶意 cause 拖住：超过 8 层就不再往下翻（平台的异常不会包这么深）
        var e: Throwable = IllegalStateException(platformMessage)
        for (i in 0 until 8) e = RuntimeException("wrap$i", e)
        assertFalse(EditorEnterKeyGuard.isFocusSearchFailure(e))
        // 第 8 层以内仍然认得出
        var inner: Throwable = IllegalStateException(platformMessage)
        for (i in 0 until 7) inner = RuntimeException("wrap$i", inner)
        assertTrue(EditorEnterKeyGuard.isFocusSearchFailure(inner))
    }
}
