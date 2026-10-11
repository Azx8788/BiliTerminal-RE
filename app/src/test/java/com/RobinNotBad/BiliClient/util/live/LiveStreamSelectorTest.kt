package com.RobinNotBad.BiliClient.util.live

import com.RobinNotBad.BiliClient.model.LivePlayInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [LiveStreamSelector] 的纯 JVM 单测。
 *
 * 这里锁住三件容易静默出错的事：
 * 1. **第一个候选必须与旧行为一字不差**（`stream[0].format[0].codec[0].url_info[0]` 的
 *    `host + base_url + extra`）——否则一上线就会换掉所有人的默认线路；
 * 2. HLS 那一路要真的能出现在候选表里（旧代码从不用它，这是"永远 FLV 单长连接"的根源）；
 * 3. 接口返回半截结构（未开播/已下播/风控）时不能崩，只能少给候选。
 */
class LiveStreamSelectorTest {

    private fun urlInfo(host: String?, extra: String? = "") = LivePlayInfo.UrlInfo().apply {
        this.host = host
        this.extra = extra
    }

    private fun codec(
        name: String,
        baseUrl: String?,
        urlInfos: List<LivePlayInfo.UrlInfo>?
    ) = LivePlayInfo.Codec().apply {
        codec_name = name
        base_url = baseUrl
        url_info = urlInfos
    }

    private fun format(name: String, codecs: List<LivePlayInfo.Codec>?) =
        LivePlayInfo.Format().apply {
            format_name = name
            codec = codecs
        }

    private fun protocol(name: String, formats: List<LivePlayInfo.Format>?) =
        LivePlayInfo.ProtocolInfo().apply {
            protocol_name = name
            format = formats
        }

    private fun playInfo(streams: List<LivePlayInfo.ProtocolInfo>?): LivePlayInfo =
        LivePlayInfo().apply {
            playUrl = LivePlayInfo.PlayUrl().apply { stream = streams }
        }

    /** 接口真实形态：stream[0] = http_stream/flv（多线路），stream[1] = http_hls/ts */
    private fun realWorldPlayInfo() = playInfo(
        listOf(
            protocol(
                "http_stream",
                listOf(
                    format(
                        "flv",
                        listOf(
                            codec(
                                "avc",
                                "/live-bvc/123/base.m3u8?",
                                listOf(
                                    urlInfo("https://cdn-a.example.com", "extraA"),
                                    urlInfo("https://cdn-b.example.com", "extraB"),
                                    urlInfo("https://cdn-c.example.com", "extraC")
                                )
                            )
                        )
                    )
                )
            ),
            protocol(
                "http_hls",
                listOf(
                    format(
                        "ts",
                        listOf(
                            codec(
                                "avc",
                                "/live-bvc/123/hls.m3u8?",
                                listOf(urlInfo("https://hls-a.example.com", "extraH"))
                            )
                        )
                    )
                )
            )
        )
    )

    @Test
    fun `首选候选与旧行为完全一致`() {
        val first = LiveStreamSelector.first(realWorldPlayInfo())
        assertEquals("https://cdn-a.example.com/live-bvc/123/base.m3u8?extraA", first?.url)
        assertEquals("http_stream", first?.protocolName)
        assertEquals("flv", first?.formatName)
        assertEquals("avc", first?.codecName)
        assertEquals(0, first?.hostIndex)
        assertFalse(first!!.isHls)
    }

    @Test
    fun `FLV 各线路排在前 HLS 排在后`() {
        val list = LiveStreamSelector.candidates(realWorldPlayInfo())
        assertEquals(4, list.size)
        assertEquals(
            listOf(
                "https://cdn-a.example.com/live-bvc/123/base.m3u8?extraA",
                "https://cdn-b.example.com/live-bvc/123/base.m3u8?extraB",
                "https://cdn-c.example.com/live-bvc/123/base.m3u8?extraC",
                "https://hls-a.example.com/live-bvc/123/hls.m3u8?extraH"
            ),
            list.map { it.url }
        )
        assertEquals(listOf(0, 1, 2, 0), list.map { it.hostIndex })
        assertEquals(listOf(false, false, false, true), list.map { it.isHls })
        assertEquals("ts", list[3].formatName)
        assertEquals("http_hls", list[3].protocolName)
    }

    @Test
    fun `即使接口把 HLS 排在前面 FLV 也要排到前面`() {
        val hls = protocol(
            "http_hls",
            listOf(format("ts", listOf(codec("avc", "/h.m3u8?", listOf(urlInfo("https://hls.example.com"))))))
        )
        val flv = protocol(
            "http_stream",
            listOf(format("flv", listOf(codec("avc", "/f.flv?", listOf(urlInfo("https://flv.example.com"))))))
        )
        val list = LiveStreamSelector.candidates(playInfo(listOf(hls, flv)))
        assertEquals(listOf("https://flv.example.com/f.flv?", "https://hls.example.com/h.m3u8?"), list.map { it.url })
    }

    @Test
    fun `同一 codec 内多个 codec 条目按接口顺序追加`() {
        val p = playInfo(
            listOf(
                protocol(
                    "http_stream",
                    listOf(
                        format(
                            "flv",
                            listOf(
                                codec("avc", "/avc?", listOf(urlInfo("https://a.example.com"))),
                                codec("hevc", "/hevc?", listOf(urlInfo("https://b.example.com")))
                            )
                        )
                    )
                )
            )
        )
        val list = LiveStreamSelector.candidates(p)
        assertEquals(listOf("https://a.example.com/avc?", "https://b.example.com/hevc?"), list.map { it.url })
        assertEquals(listOf("avc", "hevc"), list.map { it.codecName })
    }

    @Test
    fun `fmp4 也算 HLS`() {
        val p = playInfo(
            listOf(
                protocol(
                    "http_hls",
                    listOf(
                        format("ts", listOf(codec("avc", "/ts?", listOf(urlInfo("https://t.example.com"))))),
                        format("fmp4", listOf(codec("avc", "/fmp4?", listOf(urlInfo("https://f.example.com")))))
                    )
                )
            )
        )
        val list = LiveStreamSelector.candidates(p)
        assertEquals(listOf(true, true), list.map { it.isHls })
        assertEquals(listOf("ts", "fmp4"), list.map { it.formatName })
    }

    @Test
    fun `重复地址只保留一条`() {
        val p = playInfo(
            listOf(
                protocol(
                    "http_stream",
                    listOf(
                        format(
                            "flv",
                            listOf(
                                codec("avc", "/same?", listOf(urlInfo("https://dup.example.com"))),
                                codec("avc", "/same?", listOf(urlInfo("https://dup.example.com")))
                            )
                        )
                    )
                )
            )
        )
        assertEquals(1, LiveStreamSelector.candidates(p).size)
    }

    @Test
    fun `空白 host 或 base_url 被跳过`() {
        val p = playInfo(
            listOf(
                protocol(
                    "http_stream",
                    listOf(
                        format(
                            "flv",
                            listOf(
                                codec("avc", "/ok?", listOf(urlInfo(""), urlInfo(null), urlInfo("https://ok.example.com"))),
                                codec("avc", "   ", listOf(urlInfo("https://nope.example.com"))),
                                codec("avc", null, listOf(urlInfo("https://nope2.example.com")))
                            )
                        )
                    )
                )
            )
        )
        val list = LiveStreamSelector.candidates(p)
        assertEquals(listOf("https://ok.example.com/ok?"), list.map { it.url })
        // hostIndex 仍然按 url_info 的真实下标走（跳过的条目也占位）
        assertEquals(listOf(2), list.map { it.hostIndex })
    }

    @Test
    fun `extra 为 null 时按空串拼接`() {
        val p = playInfo(
            listOf(
                protocol(
                    "http_stream",
                    listOf(format("flv", listOf(codec("avc", "/x.flv", listOf(urlInfo("https://n.example.com", null))))))
                )
            )
        )
        assertEquals("https://n.example.com/x.flv", LiveStreamSelector.first(p)?.url)
    }

    @Test
    fun `半截结构不崩溃只返回空或部分候选`() {
        assertEquals(emptyList<LiveStreamSelector.Candidate>(), LiveStreamSelector.candidates(null))
        assertNull(LiveStreamSelector.first(null))
        // playUrl 为 null
        assertEquals(emptyList<LiveStreamSelector.Candidate>(), LiveStreamSelector.candidates(LivePlayInfo()))
        // stream 为 null / 空
        assertEquals(emptyList<LiveStreamSelector.Candidate>(), LiveStreamSelector.candidates(playInfo(null)))
        assertEquals(emptyList<LiveStreamSelector.Candidate>(), LiveStreamSelector.candidates(playInfo(emptyList())))
        // codec / url_info 为 null
        val p = playInfo(
            listOf(
                protocol("http_stream", listOf(format("flv", null))),
                protocol("http_stream", listOf(format("flv", listOf(codec("avc", "/x?", null)))))
            )
        )
        assertEquals(emptyList<LiveStreamSelector.Candidate>(), LiveStreamSelector.candidates(p))
    }

    @Test
    fun `candidateAt 越界返回 null`() {
        assertEquals(2, LiveStreamSelector.candidateAt(realWorldPlayInfo(), 2)?.hostIndex)
        assertNull(LiveStreamSelector.candidateAt(realWorldPlayInfo(), 4))
        assertNull(LiveStreamSelector.candidateAt(realWorldPlayInfo(), -1))
    }

    @Test
    fun `routeCandidates 只列 FLV 线路 纯 HLS 时退化`() {
        // 有 FLV：只给 FLV 的三条线路
        val routes = LiveStreamSelector.routeCandidates(realWorldPlayInfo())
        assertEquals(3, routes.size)
        assertTrue(routes.all { !it.isHls })

        // 纯 HLS：不能空着
        val hlsOnly = playInfo(
            listOf(
                protocol(
                    "http_hls",
                    listOf(format("ts", listOf(codec("avc", "/h.m3u8?", listOf(urlInfo("https://h1.example.com"), urlInfo("https://h2.example.com"))))))
                )
            )
        )
        assertEquals(2, LiveStreamSelector.routeCandidates(hlsOnly).size)
        assertEquals(emptyList<LiveStreamSelector.Candidate>(), LiveStreamSelector.routeCandidates(null))
    }

    @Test
    fun `orderedForFailover 把选中的路线排到最前且不丢不重`() {
        val p = realWorldPlayInfo()
        val ordered = LiveStreamSelector.orderedForFailover(p, 1)
        assertEquals(4, ordered.size)
        assertEquals("https://cdn-b.example.com/live-bvc/123/base.m3u8?extraB", ordered[0].url)
        // 其余三条仍在（顺序保持原样）
        assertEquals(
            listOf(
                "https://cdn-a.example.com/live-bvc/123/base.m3u8?extraA",
                "https://cdn-c.example.com/live-bvc/123/base.m3u8?extraC",
                "https://hls-a.example.com/live-bvc/123/hls.m3u8?extraH"
            ),
            ordered.drop(1).map { it.url }
        )
        // 越界退回首选
        assertEquals(
            "https://cdn-a.example.com/live-bvc/123/base.m3u8?extraA",
            LiveStreamSelector.orderedForFailover(p, 99)[0].url
        )
        assertEquals(emptyList<LiveStreamSelector.Candidate>(), LiveStreamSelector.orderedForFailover(null, 0))
    }

    @Test
    fun `isHls 判定与大小写无关`() {
        assertTrue(LiveStreamSelector.isHls("http_hls", "ts"))
        assertTrue(LiveStreamSelector.isHls("HTTP_HLS", "TS"))
        assertTrue(LiveStreamSelector.isHls("http_hls", "fmP4"))
        assertTrue(LiveStreamSelector.isHls(null, "ts"))
        assertFalse(LiveStreamSelector.isHls("http_stream", "flv"))
        assertFalse(LiveStreamSelector.isHls(null, null))
        assertTrue(LiveStreamSelector.isFlv("FLV"))
        assertFalse(LiveStreamSelector.isFlv("ts"))
    }

    @Test
    fun `单协议单线路时退化为与旧实现相同的唯一地址`() {
        val p = playInfo(
            listOf(
                protocol(
                    "http_stream",
                    listOf(format("flv", listOf(codec("avc", "/only.flv?", listOf(urlInfo("https://only.example.com", "e"))))))
                )
            )
        )
        val list = LiveStreamSelector.candidates(p)
        assertEquals(1, list.size)
        assertEquals("https://only.example.com/only.flv?e", list[0].url)
    }
}
