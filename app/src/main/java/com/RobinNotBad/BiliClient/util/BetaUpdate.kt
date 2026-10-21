package com.RobinNotBad.BiliClient.util

import org.json.JSONObject
import java.util.Locale

/**
 * 内测包分享站（文件快递柜 FileCodeBox）相关的**纯函数**：链接拼接、内测码校验、分享详情解析。
 *
 * 为什么单独抽出来：这里的逻辑会被 JVM 单测直接调用，不能读资源、不能碰 Context
 * （见 AGENTS.md「请求与解析分离」）。
 *
 * 分享站有两个域名，两个都是同一套 FileCodeBox 部署：
 * - `file.zsapp.asia`
 * - `share.lanol.cn`（用户给出的「拼接链接」用的就是它）
 *
 * 内测码对应哪个域名的包里，事先不知道，所以 [HOSTS] 按顺序逐个探测（见 `UpdateManager.resolveBetaShare`）。
 */
object BetaUpdate {

    /** 探测顺序：哪个域名有包就从哪个下载。 */
    val HOSTS = listOf("https://file.zsapp.asia", "https://share.lanol.cn")

    /** 「拼接链接」固定用这个域名。 */
    const val SHARE_HOST = "https://share.lanol.cn"

    private const val SELECT_PATH = "/share/select/"

    /** 内测码：纯数字，长度兜底给到 20（分享站码一般 5 位，但别写死）。 */
    private val CODE_REGEX = Regex("^\\d{1,20}$")

    /** 分享站返回的文件信息。 */
    data class ShareFile(
        val name: String,
        /** 字节数，取不到时为 0。 */
        val size: Long,
        /** 已补全成可直接 GET 的绝对 URL。 */
        val downloadUrl: String
    )

    sealed class Result {
        data class Found(val file: ShareFile, val host: String) : Result()

        /** 该域名上没有这个包、或包里不是可下载的文件。message 可直接展示。 */
        data class NotFound(val message: String) : Result()
    }

    /**
     * 校验并规整内测码：去掉首尾空白后必须全是数字。不合法返回 null。
     */
    fun normalizeCode(raw: String): String? {
        val code = raw.trim()
        return if (CODE_REGEX.matches(code)) code else null
    }

    /**
     * 拼接用户可见的分享链接，形如 `https://share.lanol.cn/#/?code=12345`。
     */
    fun buildShareUrl(code: String, host: String = SHARE_HOST): String = "$host/#/?code=$code"

    /**
     * 分享文件名是否像安装包。分享站不校验上传内容，内测码可能对应一张图片或一段文本，
     * 只有 `.apk` 才值得下载——否则下载完也只能被 [ApkVerifier] 拒掉。
     */
    fun isApkFileName(name: String): Boolean = name.trim().endsWith(".apk", ignoreCase = true)

    /**
     * 分享文件名是否像纯文本（`.txt`）。内测码对应的 txt 不下载成安装包，
     * 而是把内容当公告展示（见 `UpdateActivity` / `ShowTextActivity`）。
     */
    fun isTextFileName(name: String): Boolean = name.trim().endsWith(".txt", ignoreCase = true)

    /** `POST /share/select/` 的地址。 */
    fun selectUrl(host: String): String = host + SELECT_PATH

    /** `POST /share/select/` 的 JSON 请求体。 */
    fun buildSelectBody(code: String): String = JSONObject().put("code", code).toString()

    /**
     * 解析 `POST /share/select/` 的响应。
     *
     * 成功形如：
     * `{"code":200,"message":"ok","detail":{"name":"app.apk","size":123,"download_url":"/share/download?key=…&code=…"}}`
     *
     * 失败形如：`{"code":404,"message":"ok","detail":"文件不存在"}`
     *
     * `download_url` 可能是相对路径（本站下载）或绝对 URL（外部存储），统一经 [absoluteUrl] 补全。
     */
    fun parseSelectResponse(jsonStr: String, host: String): Result {
        val json = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            return Result.NotFound("返回内容不是合法的 JSON")
        }

        val status = json.optInt("code", -1)
        if (status != 200) {
            val detail = json.optString("detail", "")
            val message = detail.ifBlank { json.optString("message", "") }
            return Result.NotFound(message.ifBlank { "内测码无效（code=$status）" })
        }

        val detail = json.optJSONObject("detail")
            ?: return Result.NotFound("分享内容为空")

        val rawUrl = detail.optString("download_url", "")
        if (rawUrl.isBlank()) {
            return Result.NotFound("该内测码不是可下载的文件")
        }

        val name = detail.optString("name", "").trim()
        if (detail.optBoolean("is_text", false)) {
            // 文本分享（text/content）没有 download_url，不能当安装包用
            return Result.NotFound("该内测码是文本分享，不是安装包")
        }
        if (name.isNotBlank() && !isApkFileName(name) && !isTextFileName(name)) {
            // 分享站不校验上传内容，可能是图片/文档；别下载完才发现不是安装包
            return Result.NotFound("该内测码对应的文件不是安装包")
        }

        val size = detail.optLong("size", 0L)
        return Result.Found(ShareFile(name.ifBlank { "内测包" }, size, absoluteUrl(host, rawUrl)), host)
    }

    /** 把分享站返回的下载地址补全成绝对 URL。 */
    fun absoluteUrl(host: String, url: String): String = when {
        url.startsWith("https://") || url.startsWith("http://") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") -> host + url
        else -> "$host/$url"
    }

    /**
     * 内测 `.txt` 的正文解码：UTF-8，并去掉可能的 BOM（分享站导出的 txt 常带 BOM，
     * 不去掉的话正文第一行前会多一个不可见字符）。
     */
    fun decodeText(bytes: ByteArray): String = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")

    /** 把字节数格式化成人话，如 `12.3 MB`。 */
    fun formatSize(bytes: Long): String = when {
        bytes <= 0L -> "未知大小"
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
