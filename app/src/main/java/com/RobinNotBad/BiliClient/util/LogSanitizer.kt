package com.RobinNotBad.BiliClient.util

import java.util.regex.Pattern

/**
 * 日志文本脱敏：把 `键=值` 里命中敏感 / 噪音键名的**值**替换掉，键名保留。
 *
 * <p>为什么要它：内存日志缓冲（[Logu] 的环形缓冲）会把最近若干行日志原样留在内存里，
 * 崩溃 / 反馈时导出给用户。而这些日志行里可能夹着 `access_key=…`、`SESSDATA=…` 这类
 * **可直接冒用身份的凭证**——写进导出文本等于把会话明文送人。脱敏必须在**入缓冲之前**做，
 * 否则事后补救等于凭证已经进过内存快照。
 *
 * <p>为什么按 `键=值` 而不是按 URL 解析：日志行是**任意文本**——可能整行是一条 URL，
 * 也可能只有一小段带参数的片段，甚至根本不是 URL（Cookie 串、请求体摘要）。按 URL 解析
 * 会漏掉后两种；按 `键=值` 做文本替换则三种都能覆盖。
 *
 * <p>占位符与 [NetWorkUtil.maskUrl] 的既有约定**完全一致**（敏感 → `***`，噪音 → `…`），
 * 两处脱敏看起来才是同一套语义。键名清单也直接复用 NetWorkUtil 的常量，不各写一份。
 *
 * <p>**不依赖任何 `android.*` 类**——本类必须能在纯 JVM 单测里跑（见 `LogSanitizerTest`）。
 */
object LogSanitizer {

    /** 敏感值的占位符。与 [NetWorkUtil.maskUrl] 保持一致，别改。 */
    private const val MASK_SENSITIVE = "***"

    /** 噪音值的占位符。与 [NetWorkUtil.maskUrl] 保持一致，别改。 */
    private const val MASK_NOISE = "…"

    /**
     * 全部受管键名（敏感 + 噪音），供快速路径预检。
     *
     * <p>直接引用 [NetWorkUtil] 的常量，保证与 HTTP 追踪用同一份清单。
     */
    private val ALL_KEYS: Array<String> =
        NetWorkUtil.SENSITIVE_QUERY_KEYS + NetWorkUtil.NOISE_QUERY_KEYS

    /** [ALL_KEYS] 的 `键名=` 形式，供快速路径预检用（预先拼好，避免热路径上每次分配字符串）。 */
    private val ALL_KEYS_WITH_EQ: Array<String> = ALL_KEYS.map { it + "=" }.toTypedArray()

    /** 小写化的敏感键集合，用于判定命中的键该用哪个占位符。 */
    private val SENSITIVE_LOWER: Set<String> =
        NetWorkUtil.SENSITIVE_QUERY_KEYS.map { it.lowercase() }.toSet()

    /**
     * `键=值` 匹配正则：`\b(键名)=值`。
     *
     * <p>值的结束符取 `&`（URL 查询串）、`;`（Cookie 串）、空白、行尾——日志行可能出现在
     * 这四种上下文里的任意一种，取这四者能全覆盖，不会把一个值连到下一个键上。
     *
     * <p>`\b` 词边界（`_` 算词字符）保证不会从别的标识符中间起匹配，例如 `refresh_token`
     * 不会被当成 `token` 再打一次码；键名按长度降序拼接，避免短键名抢先匹配长键名的前缀。
     */
    private val PATTERN: Pattern = Pattern.compile(
        buildPattern(),
        Pattern.CASE_INSENSITIVE
    )

    /**
     * 对任意文本脱敏：命中敏感键 → 值替换为 `***`，命中噪音键 → 值替换为 `…`，键名原样保留。
     *
     * <p>**快速路径**：先用 `String.contains(键名, ignoreCase = true)` 预检有没有任何一个
     * 受管键名出现。绝大多数日志行一个都没有，直接原样返回，**不跑正则**——这是每行日志都要
     * 走的热路径，手表端 CPU 弱，无条件跑正则的代价不能接受。
     *
     * @param text 待脱敏文本
     * @return 脱敏后的文本；入参为 null 时返回空串（与 [NetWorkUtil.maskUrl] 的 null 语义一致）
     */
    @JvmStatic
    fun sanitize(text: String?): String {
        if (text == null) return ""
        if (text.isEmpty()) return text
        // 快速路径：连一个受管键名都没出现，直接原样返回，不碰正则
        if (!containsAnyKey(text)) return text
        val matcher = PATTERN.matcher(text)
        if (!matcher.find()) return text
        val sb = StringBuilder(text.length)
        var last = 0
        do {
            sb.append(text, last, matcher.start())
            val key = matcher.group(1)
            val placeholder = if (isSensitive(key)) MASK_SENSITIVE else MASK_NOISE
            sb.append(key).append('=').append(placeholder)
            last = matcher.end()
        } while (matcher.find())
        sb.append(text, last, text.length)
        return sb.toString()
    }

    /**
     * 预检：文本里是否出现了任何一个受管键名的**键值对形式**（`键名=`，大小写不敏感）。
     *
     * <p>刻意连等号一起查，而不是只查键名。正则本身要求 `键名=`，所以这样预检**不会漏**；
     * 但只查键名的话，日志里的 `design` / `signature` / `tokenizer` 这类普通英文词会天天
     * 误触发慢路径去跑正则——而它们根本不是键值对。手表端热路径上这个差别值得较真。
     */
    private fun containsAnyKey(text: String): Boolean {
        for (key in ALL_KEYS_WITH_EQ) {
            if (text.contains(key, ignoreCase = true)) return true
        }
        return false
    }

    /** 命中的键名是否属于敏感清单（大小写不敏感）。 */
    private fun isSensitive(key: String): Boolean = SENSITIVE_LOWER.contains(key.lowercase())

    /** 拼接正则：`\b(键1|键2|…)=([^&;\s]*)`，键名按长度降序、逐个转义。 */
    private fun buildPattern(): String {
        val keys = ALL_KEYS.sortedByDescending { it.length }.joinToString("|") { Pattern.quote(it) }
        return "\\b($keys)=([^&;\\s]*)"
    }
}
