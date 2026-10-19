package com.RobinNotBad.BiliClient.api

import com.RobinNotBad.BiliClient.util.UiText

/**
 * API 层「操作失败」类文案的唯一出处（26.10.19 建立）。
 *
 * 为什么不放 `strings.xml`：API 层这些函数是**纯函数**，跟随 app/src/test 下的 JVM 单测一起跑，
 * 单测环境没有 Context，`BiliTerminal.context!!.getString(...)` 会直接空指针。
 * 所以服务端错误码 → 人话 的映射留在代码里，全工程只此一处；界面上要改这几句就改这里。
 *
 * 另有 `xxxErrorMsg(code)` 这类**按错误码分支**的文案（"还没有登录喵~" 等）仍写在各自 API 类里，
 * 它们本来就因 API 而异，不属于重复。
 */
object ApiErrorText {

    /** 「操作失败（错误码 10001）」——把服务端错误码原样带上，用户报障时能对上日志。 */
    @JvmStatic
    fun opFailed(code: Any?): String = "操作失败（错误码 $code）"

    /** 「操作失败（-101）」——已知码已被前面分支吃掉，兜底时只带码本身。 */
    @JvmStatic
    fun opFailedBrief(code: Any?): String = "操作失败（$code）"

    /** 「操作失败：你的账号已被封禁」——接口给了可读原因时用它。 */
    @JvmStatic
    fun opFailedWithDetail(detail: Any?): String = "操作失败：$detail"
}