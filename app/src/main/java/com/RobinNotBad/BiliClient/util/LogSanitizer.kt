package com.RobinNotBad.BiliClient.util

import java.util.regex.Pattern

/**
 * 日志文本脱敏：把受管键名对应的**值**替换掉，键名保留。
 *
 * <p>为什么要它：内存日志缓冲（[Logu] 的环形缓冲）会把最近若干行日志原样留在内存里，
 * 崩溃 / 反馈时导出给用户。而这些日志行里可能夹着 `access_key=…`、`SESSDATA=…` 这类
 * **可直接冒用身份的凭证**——写进导出文本等于把会话明文送人。脱敏必须在**入缓冲之前**做，
 * 否则事后补救等于凭证已经进过内存快照。
 *
 * <p>为什么按文本替换而不是按 URL 解析：日志行是**任意文本**——可能整行是一条 URL，
 * 也可能只有一小段带参数的片段，甚至根本不是 URL（Cookie 串、请求体 / 响应体预览）。
 * 按 URL 解析会把后两种整类漏掉。
 *
 * <p>覆盖的四种形态（都是日志里实际见过的）：
 * 1. `键=值`：查询串（`&` 分隔）、Cookie 串（`;` 分隔）、空白或行尾结束的片段；
 * 2. URL 编码形态 `键%3D值`：整段查询串被当成参数值再编码时，等号会变成 `%3D`；
 * 3. JSON 形态 `"键":"值"` / `"键": 数字`：HTTP 追踪里请求体 / 响应体的预览大量是这种；
 * 4. 裸的 11 位手机号（`1[3-9]` 开头）：用户填进反馈联系方式、又被日志带出来的情形。
 *
 * <p>占位符与 [NetWorkUtil.maskUrl] 的既有约定**完全一致**（敏感 → `***`，噪音 → `…`），
 * 两处脱敏看起来才是同一套语义。键名清单直接复用 NetWorkUtil 的常量，不各写一份。
 *
 * <p>**不依赖任何 `android.*` 类**——本类必须能在纯 JVM 单测里跑（见 `LogSanitizerTest`）。
 */
object LogSanitizer {

    /** 敏感值的占位符。与 [NetWorkUtil.maskUrl] 保持一致，别改。 */
    private const val MASK_SENSITIVE = "***"

    /** 噪音值的占位符。与 [NetWorkUtil.maskUrl] 保持一致，别改。 */
    private const val MASK_NOISE = "…"

    /**
     * [NetWorkUtil.SENSITIVE_QUERY_KEYS] 之外、日志里同样必须打码的键。
     *
     * <p>为什么要分两处：NetWorkUtil 的清单同时喂给 [NetWorkUtil.maskUrl]，改动它等于改
     * HTTP 追踪里 URL 的脱敏口径；而下面这几个大多只出现在**日志文本**里——
     * `access_token` 是另一套接口的凭证，`refresh_csrf` 是刷新用的第二枚 CSRF，
     * `buvid3` / `buvid4` 是设备指纹 Cookie，`phone` / `mobile` 是用户可能填进反馈的联系方式。
     *
     * <p>**别把这里当第二份真相**：NetWorkUtil 里已有的键仍然只在那一处维护。
     */
    private val EXTRA_SENSITIVE_KEYS = arrayOf(
        "access_token", "refresh_csrf", "buvid3", "buvid4", "phone", "mobile"
    )

    /** 敏感键（值 → `***`）：NetWorkUtil 的清单 + 日志侧补充。 */
    private val SENSITIVE_KEYS: Array<String> =
        NetWorkUtil.SENSITIVE_QUERY_KEYS + EXTRA_SENSITIVE_KEYS

    /** 全部受管键名（敏感 + 噪音），供快速路径预检与正则拼装。 */
    private val ALL_KEYS: Array<String> = SENSITIVE_KEYS + NetWorkUtil.NOISE_QUERY_KEYS

    /** 小写化的敏感键集合，用于判定命中的键该用哪个占位符。 */
    private val SENSITIVE_LOWER: Set<String> = SENSITIVE_KEYS.map { it.lowercase() }.toSet()

    /**
     * 键名按长度降序拼成的正则分支。
     *
     * <p>长度降序让长键名先匹配：`refresh_token` 不会被 `token` 抢先切掉前半截。
     */
    private val KEY_ALTERNATION: String =
        ALL_KEYS.sortedByDescending { it.length }.joinToString("|") { Pattern.quote(it) }

    /**
     * `键=值` 匹配正则：`\b(键名)=值`。
     *
     * <p>值的结束符取 `&`（URL 查询串）、`;`（Cookie 串）、空白、行尾——日志行可能出现在
     * 这四种上下文里的任意一种，取这四者能全覆盖，不会把一个值连到下一个键上。
     *
     * <p>`\b` 词边界（`_` 算词字符）保证不会从别的标识符中间起匹配，例如
     * `access_token` 里的 `token` 不会再被单独打一次码。
     */
    private val KV_PATTERN: Pattern = Pattern.compile(
        "\\b($KEY_ALTERNATION)=([^&;\\s]*)",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * URL 编码形态：`键%3D值`。
     *
     * <p>典型来源是把一整段查询串当参数值再编码（`data=access_key%3D…%26csrf%3D…`）；
     * 只看 `=` 的旧规则对它是完全瞎的。
     */
    private val ENCODED_KV_PATTERN: Pattern = Pattern.compile(
        "\\b($KEY_ALTERNATION)%3D([^&;\\s]*)",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * JSON 形态：`"键":"值"` 或 `"键": 数字/布尔`。
     *
     * <p>字符串值允许转义（`\\"`），数字 / 布尔走第二分支；替换后统一成
     * `"键":"***"`——日志文本里只要求「看不出原值」，不要求保持 JSON 类型。
     */
    private val JSON_PATTERN: Pattern = Pattern.compile(
        "\"($KEY_ALTERNATION)\"\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|[^,}\\s]+)",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * 裸手机号：11 位、`1` 开头、第二位 `3`–`9`。
     *
     * <p>前后用「不能挨着数字」而不是 `\b`：`\b` 对中文、下划线都算边界，
     * 从 13 位时间戳里切出 11 位手机号正是要排除的情形。
     */
    private val PHONE_PATTERN: Pattern = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)")

    /**
     * 对任意文本脱敏：命中敏感键 → 值替换为 `***`，命中噪音键 → 值替换为 `…`，键名原样保留。
     *
     * <p>**快速路径**：先用 `String.contains(键名, ignoreCase = true)` 预检有没有任何一个
     * 受管键名出现。绝大多数日志行一个都没有，**三个键值对正则都不碰**；裸手机号那一遍另有
     * 「长度 ≥ 11 且含 1」的廉价预检。这是每行日志都要走的热路径，手表端 CPU 弱，
     * 无条件跑四个正则的代价不能接受。
     *
     * @param text 待脱敏文本
     * @return 脱敏后的文本；入参为 null 时返回空串（与 [NetWorkUtil.maskUrl] 的 null 语义一致）
     */
    @JvmStatic
    fun sanitize(text: String?): String {
        if (text == null) return ""
        if (text.isEmpty()) return text
        // 快速路径：连一个受管键名都没出现时不碰三个「键值对」正则；但裸手机号与键名无关，
        // 仍要走一遍（它自带「11 位且含 1」的廉价预检，没有数字的行会被直接放过）。
        val keyed = if (containsAnyKey(text)) maskKeyedForms(text) else text
        return maskPhones(keyed)
    }

    /** 依次处理三种「带键名」的形态；每种没命中时原样返回入参，不额外分配。 */
    private fun maskKeyedForms(text: String): String {
        var out = rewrite(text, KV_PATTERN) { key -> "$key=${maskFor(key)}" }
        out = rewrite(out, ENCODED_KV_PATTERN) { key -> "$key%3D${maskFor(key)}" }
        out = rewrite(out, JSON_PATTERN) { key -> "\"$key\":\"${maskFor(key)}\"" }
        return out
    }

    /** 把每个匹配整体换成 [replacement]（入参是正则 group(1)，即键名）。 */
    private inline fun rewrite(text: String, pattern: Pattern, replacement: (String) -> String): String {
        val matcher = pattern.matcher(text)
        if (!matcher.find()) return text
        val sb = StringBuilder(text.length + 16)
        var last = 0
        do {
            sb.append(text, last, matcher.start())
            sb.append(replacement(matcher.group(1)))
            last = matcher.end()
        } while (matcher.find())
        sb.append(text, last, text.length)
        return sb.toString()
    }

    /** 裸手机号也要打码：它与键名无关，所以单独一遍。 */
    private fun maskPhones(text: String): String {
        // 省掉绝大多数行：手机号必须是 11 位且以 1 开头，短行 / 没有 1 的行直接返回
        if (text.length < 11 || text.indexOf('1') < 0) return text
        val matcher = PHONE_PATTERN.matcher(text)
        if (!matcher.find()) return text
        val sb = StringBuilder(text.length)
        var last = 0
        do {
            sb.append(text, last, matcher.start())
            sb.append(MASK_SENSITIVE)
            last = matcher.end()
        } while (matcher.find())
        sb.append(text, last, text.length)
        return sb.toString()
    }

    /**
     * 预检：文本里是否出现了任何一个受管键名（大小写不敏感），命中一个就返回。
     *
     * <p>刻意**不再要求键名后面跟等号**：JSON 形态里键名后面跟的是引号 + 冒号，
     * 只查 `键=` 会让 `"access_key":"…"` 整类漏掉。误判（例如 `design` 里含 `sign`）
     * 只会让这一行多跑几次正则，不会改坏文本。
     */
    private fun containsAnyKey(text: String): Boolean {
        for (key in ALL_KEYS) {
            if (text.contains(key, ignoreCase = true)) return true
        }
        return false
    }

    /** 命中键该用哪个占位符：敏感 → `***`，噪音 → `…`。 */
    private fun maskFor(key: String): String = if (isSensitive(key)) MASK_SENSITIVE else MASK_NOISE

    /** 命中的键名是否属于敏感清单（大小写不敏感）。 */
    private fun isSensitive(key: String): Boolean = SENSITIVE_LOWER.contains(key.lowercase())
}
