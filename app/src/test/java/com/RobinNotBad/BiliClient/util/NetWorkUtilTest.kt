package com.RobinNotBad.BiliClient.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetWorkUtilTest {

    private val fakePrefs = FakeSharedPreferences()

    @Before
    fun setUp() {
        SharedPreferencesUtil.sharedPreferences = fakePrefs
    }

    @After
    fun tearDown() {
        SharedPreferencesUtil.sharedPreferences = null
    }

    @Test
    fun buildGuestCookieString_removesLoginCookies_keepsGuestCookies() {
        val full = "SESSDATA=abc; bili_jct=def; DedeUserID=123; DedeUserID__ckMd5=xyz; " +
                "sid=9; buvid3=aaa; buvid4=bbb; bili_ticket=ccc; _uuid=ddd; CURRENT_FNVAL=4048"
        val guest = NetWorkUtil.buildGuestCookieString(full)

        assertFalse("应剔除SESSDATA", guest.contains("SESSDATA"))
        assertFalse("应剔除bili_jct", guest.contains("bili_jct"))
        assertFalse("应剔除DedeUserID", guest.contains("DedeUserID"))
        assertFalse("应剔除DedeUserID__ckMd5", guest.contains("DedeUserID__ckMd5"))
        assertFalse("应剔除sid", guest.contains("sid"))
        assertTrue("应保留buvid3", guest.contains("buvid3=aaa"))
        assertTrue("应保留buvid4", guest.contains("buvid4=bbb"))
        assertTrue("应保留bili_ticket", guest.contains("bili_ticket=ccc"))
        assertTrue("应保留_uuid", guest.contains("_uuid=ddd"))
    }

    @Test
    fun buildGuestCookieString_nullOrEmpty_returnsEmpty() {
        assertEquals("", NetWorkUtil.buildGuestCookieString(null))
        assertEquals("", NetWorkUtil.buildGuestCookieString(""))
    }

    // ---- pickCsrf：csrf 必须是「实时值」而不是登录那一刻的快照 ----
    // bili_jct 会随 Cookie 刷新轮换，用旧快照发 POST 会被服务端回 -111，而报错看起来像"没登录"。
    // 这几条测试锁住的就是"优先用 Cookie 里的实时值，取不到才退回快照"这条判定。

    @Test
    fun pickCsrf_liveCookieWins_overStoredSnapshot() {
        val cookies = "SESSDATA=abc; bili_jct=LIVE_TOKEN; DedeUserID=123"
        assertEquals("应优先取 Cookie 里的 bili_jct", "LIVE_TOKEN", NetWorkUtil.pickCsrf(cookies, "OLD_TOKEN"))
    }

    @Test
    fun pickCsrf_missingLiveValue_fallsBackToStoredSnapshot() {
        val cookies = "SESSDATA=abc; DedeUserID=123"
        assertEquals("Cookie 里没有 bili_jct 时应退回快照", "OLD_TOKEN", NetWorkUtil.pickCsrf(cookies, "OLD_TOKEN"))
    }

    @Test
    fun pickCsrf_nullCookieString_fallsBackToStoredSnapshot() {
        assertEquals("Cookie 串为 null 时不能抛异常", "OLD_TOKEN", NetWorkUtil.pickCsrf(null, "OLD_TOKEN"))
    }

    @Test
    fun pickCsrf_emptyLiveValue_treatedAsMissing() {
        val cookies = "SESSDATA=abc; bili_jct=; DedeUserID=123"
        assertEquals("bili_jct 为空值时同样算没有，退回快照", "OLD_TOKEN", NetWorkUtil.pickCsrf(cookies, "OLD_TOKEN"))
    }

    @Test
    fun pickCsrf_bothEmpty_returnsEmptyStringNotFallbackGarbage() {
        assertEquals("", NetWorkUtil.pickCsrf("", ""))
        assertEquals("", NetWorkUtil.pickCsrf(null, null))
        assertEquals("", NetWorkUtil.pickCsrf("SESSDATA=abc", null))
    }

    @Test
    fun pickCsrf_liveValueFoundWhenNotFirstCookie() {
        // Cookie 顺序不固定，bili_jct 可能排在很后面；这里确认不是只看第一段
        val cookies = "buvid3=a; SESSDATA=b; DedeUserID=123; DedeUserID__ckMd5=z; sid=s; bili_jct=TAIL_TOKEN"
        assertEquals("TAIL_TOKEN", NetWorkUtil.pickCsrf(cookies, "OLD_TOKEN"))
    }

    // ==================== HTTP 追踪的 URL 脱敏（26.10.14） ====================
    // 这组用例是安全回归：追踪日志会落盘、会被 agent 读走，一旦把 access_key / csrf 的**值**
    // 写进去，等于把可直接冒用身份的凭证留在设备与日志里（与审计 S2「只记 Cookie 键名」同一原则）。

    @Test
    fun maskUrl_masksCredentialQueryValues() {
        val masked = NetWorkUtil.maskUrl(
            "https://api.bilibili.com/x/test?a=1&access_key=SECRET_KEY&csrf=SECRET_JCT&b=2"
        )
        assertFalse("access_key 的值不能出现", masked.contains("SECRET_KEY"))
        assertFalse("csrf 的值不能出现", masked.contains("SECRET_JCT"))
        assertTrue("非敏感参数要原样保留，agent 靠它判断接口语义", masked.contains("a=1"))
        assertTrue(masked.contains("b=2"))
        assertEquals(
            "https://api.bilibili.com/x/test?a=1&access_key=***&csrf=***&b=2",
            masked
        )
    }

    @Test
    fun maskUrl_masksCaseInsensitively_andOtherCredentialNames() {
        val masked = NetWorkUtil.maskUrl(
            "https://api.bilibili.com/x/test?SESSDATA=S1&refresh_token=R1&APPKEY=K1&sign=G1"
        )
        assertFalse(masked.contains("S1"))
        assertFalse(masked.contains("R1"))
        assertFalse("参数名大小写不统一时同样要打码", masked.contains("K1"))
        assertFalse(masked.contains("G1"))
    }

    @Test
    fun maskUrl_keepsPathAndHashUntouched() {
        // 路径是 agent 判断"调的是哪个接口"的唯一依据，绝不能一起打码
        val url = "https://api.bilibili.com/x/web-interface/view?bvid=BV1xx411c7mD#frag"
        assertEquals(url, NetWorkUtil.maskUrl(url))
    }

    @Test
    fun maskUrl_noQueryString_returnsAsIs() {
        val url = "https://api.bilibili.com/x/web-interface/nav"
        assertEquals(url, NetWorkUtil.maskUrl(url))
        assertEquals("", NetWorkUtil.maskUrl(null))
    }

    @Test
    fun maskUrl_toleratesValuelessAndEmptyParams() {
        // 形如 `?flag`（无等号）与 `?a=`（空值）都不该让脱敏崩掉或错位
        val masked = NetWorkUtil.maskUrl("https://x.com/p?flag&a=&csrf=")
        assertEquals("https://x.com/p?flag&a=&csrf=***", masked)
    }

    @Test
    fun maskUrl_replacesWbiFingerprintBlobsWithEllipsis() {
        // 这几个是 WBI 风控的浏览器指纹 blob，单个上千字符且无排障价值。
        // 实测不过滤时一行追踪日志能到 2200+ 字符，几个页面就把 2MB 的日志滚动窗口冲掉。
        val blob = "x".repeat(1500)
        val masked = NetWorkUtil.maskUrl(
            "https://api.bilibili.com/x/web-interface/wbi/index/top/feed/rcmd" +
                    "?bvid=BV1xx&dm_img_list=$blob&dm_img_inter=$blob&dm_cover_img_str=QU5HTEU&pn=1"
        )
        assertFalse("指纹 blob 不能整段留下", masked.contains(blob))
        assertTrue("键名要保留，便于确认参数确实存在", masked.contains("dm_img_list=…"))
        assertTrue(masked.contains("dm_img_inter=…"))
        assertTrue(masked.contains("dm_cover_img_str=…"))
        assertTrue("正常参数不受影响", masked.contains("bvid=BV1xx") && masked.contains("pn=1"))
        assertTrue("过滤后要短下来", masked.length < 400)
    }

    @Test
    fun maskUrl_truncatesUnknownOversizedParams() {
        // 兜底：出现没登记在噪音表里的超长参数时，整串限长，别把日志撑爆
        val masked = NetWorkUtil.maskUrl("https://x.com/p?unknown=" + "y".repeat(5000))
        assertTrue("必须被截断", masked.length < 700)
        assertTrue("要标出截断，避免误以为参数就这么短", masked.endsWith("…(url truncated)"))
    }
}
