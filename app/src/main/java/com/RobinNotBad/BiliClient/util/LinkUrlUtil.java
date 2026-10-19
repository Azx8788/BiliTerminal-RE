package com.RobinNotBad.BiliClient.util;

import com.RobinNotBad.BiliClient.util.UiText;
import com.RobinNotBad.BiliClient.R;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.activity.search.SearchActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class LinkUrlUtil {
    public static final int TYPE_USER = -1;
    public static final int TYPE_WEB_URL = 0;
    public static final int TYPE_BVID = 1;
    public static final int TYPE_AVID = 2;
    public static final int TYPE_CVID = 3;
    public static final int TYPE_UID = 4;
    /** 话题（{@code #话题名#}）。val 形如 {@code https://m.bilibili.com/topic-detail?topic_id=1305890}。 */
    public static final int TYPE_TOPIC = 5;
    public static final Pattern BV_PATTERN = Pattern.compile("BV[A-Za-z0-9]{10}");
    public static final Pattern AV_PATTERN = Pattern.compile("av\\d{1,10}");
    public static final Pattern CV_PATTERN = Pattern.compile("cv\\d{1,10}");
    public static final Pattern UID_PATTERN = Pattern.compile("^(?i)uid\\d+$");
    public static final Pattern OPUS_PATTERN = Pattern.compile("^/opus/(\\d+)/?$");
    /** 从话题跳转链接里抠出 topic_id，形如 {@code .../topic-detail?topic_id=1305890&topic_name=xx}。 */
    public static final Pattern TOPIC_ID_PATTERN = Pattern.compile("[?&]topic_id=(\\d+)");
    /** 动态正文话题节点的跳转域名：26.10.19 实测全站动态列表/动态详情下发的都是这个站内搜索链接。 */
    public static final String TOPIC_SEARCH_DOMAIN = "search.bilibili.com";
    /** 从站内搜索链接里抠出 keyword 参数（未解码，形如 {@code keyword=%E7%BB%9D%E5%8C%BA%E9%9B%B6}）。 */
    public static final Pattern SEARCH_KEYWORD_PATTERN = Pattern.compile("[?&]keyword=([^&]*)");

    /**
     * 从话题跳转链接里解析 topic_id。
     *
     * 话题的 {@code jump_url} 形如
     * {@code https://m.bilibili.com/topic-detail?topic_id=1305890&topic_name=%E5%A4%A9%E4%BE%9D...}，
     * 我们只需要 {@code topic_id} 就能进站内话题页（{@code DynamicTopicActivity}）。
     * 解析不出来返回 0，调用方据此退化处理。
     */
    public static long parseTopicId(String jumpUrl) {
        if (jumpUrl == null) return 0L;
        Matcher matcher = TOPIC_ID_PATTERN.matcher(jumpUrl);
        if (!matcher.find()) return 0L;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 从话题节点的 jump_url 里解析「去搜索」用的关键词。
     *
     * <p><b>为什么需要它</b>：26.10.19 用真实登录态把 {@code feed/all} 与 {@code detail} 两条接口
     * 的富文本节点全扫了一遍，{@code RICH_TEXT_NODE_TYPE_TOPIC} 的 jump_url **只有一种形态**
     * {@code //search.bilibili.com/all?keyword=%E7%BB%9D%E5%8C%BA%E9%9B%B6}——既没有 topic_id
     * 也没有 rid。所以 {@link #parseTopicId} 必然返回 0，光靠它话题永远点不动。
     * 取不出 topic_id 时按服务端给的语义去站内搜索，是唯一与网页行为一致的落点
     * （"话题名反查 topic_id"实测不可行：{@code x/topic/pub/search} 只有模糊搜索，绝区零/星塔旅人
     * 这类主话题都搜不到精确同名；{@code x/topic/web/details/top} 必须传 id）。
     *
     * <p>解析不出（空串、不是 search.bilibili.com 的链接、没有 keyword、解码失败）统一返回空串，
     * 调用方据此退化成「只上色不可点」。
     */
    public static String parseTopicSearchKeyword(String jumpUrl) {
        if (jumpUrl == null || jumpUrl.isEmpty()) return "";
        if (!jumpUrl.contains(TOPIC_SEARCH_DOMAIN)) return "";
        Matcher matcher = SEARCH_KEYWORD_PATTERN.matcher(jumpUrl);
        if (!matcher.find()) return "";
        try {
            String keyword = URLDecoder.decode(matcher.group(1), "UTF-8").trim();
            return keyword;
        } catch (Exception e) {
            return "";
        }
    }

    public static void handleWebURL(Context context, String text) {
        try {
            text = (text.startsWith("http://") || text.startsWith("https://") ? text : (text.startsWith("//") ? "http:" + text : "http://" + text));
            // 很傻逼的一系列解析
            URL url = new URL(text);
            String path = url.getPath();
            int index = path.indexOf('?');
            if (index != -1) {
                path = path.substring(0, index);
            }
            String domain = url.getHost();
            if (domain.equals("b23.tv")) {
                handleShortUrl(context, text);
                return;
            }
            if (domain.matches(".*\\.bilibili\\.com$")) {
                if (!path.isEmpty()) {
                    Matcher opusMatcher = OPUS_PATTERN.matcher(path);
                    if (opusMatcher.matches()) {
                        TerminalContext.getInstance().enterOpusDetailPage(context, Long.parseLong(opusMatcher.group(1)));
                        return;
                    }
                    String lastPathItem = path.replaceAll(".*/([^/]+)/?$", "$1");
                    if (domain.equals("space.bilibili.com")) {
                        lastPathItem = "UID" + lastPathItem;
                    }
                    if (domain.equals("search.bilibili.com")) {
                        Intent intent = new Intent(context, SearchActivity.class);
                        intent.putExtra("keyword", URLDecoder.decode(url.getQuery().replace("keyword=", "")));
                        context.startActivity(intent);
                        return;
                    }
                    Pair<String, Integer> parse = parseId(lastPathItem);
                    if (parse != null) {
                        String val = parse.first;
                        int type = parse.second;
                        switch (type) {
                            case TYPE_BVID:
                                TerminalContext.getInstance().enterVideoDetailPage(context, val);
                                return;
                            case TYPE_AVID:
                                TerminalContext.getInstance().enterVideoDetailPage(context, Long.parseLong(val.replace("av", "")));
                                return;
                            case TYPE_CVID:
                                TerminalContext.getInstance().enterArticleDetailPage(context, Long.parseLong(val.replace("cv", "")));
                                return;
                            case TYPE_UID:
                                BiliTerminal.jumpToUser(context, Long.parseLong(val.replaceFirst("(?i)^uid", "")));
                                return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            MsgUtil.err(e);
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(text)));
        } catch (ActivityNotFoundException e) {
            MsgUtil.showMsg(UiText.of("没有可处理此链接的应用！"));
        } catch (Throwable th) {
            MsgUtil.err(th);
        }
    }

    public static void handleId(Context context, String text) {
        Pair<String, Integer> parse = parseId(text);
        if (parse != null) {
            String val = parse.first;
            int type = parse.second;
            switch (type) {
                case TYPE_BVID:
                    TerminalContext.getInstance().enterVideoDetailPage(context, val);
                    break;
                case TYPE_AVID:
                    TerminalContext.getInstance().enterVideoDetailPage(context, Long.parseLong(val.replace("av", "")));
                    break;
                case TYPE_CVID:
                    TerminalContext.getInstance().enterArticleDetailPage(context, Long.parseLong(val.replace("cv", "")));
                    break;
                case TYPE_UID:
                    BiliTerminal.jumpToUser(context, Long.parseLong(val.replaceFirst("(?i)^uid", "")));
                    break;
                case TYPE_WEB_URL:
                    handleWebURL(context, val);
                    break;
            }
        }
    }

    private static void handleShortUrl(Context context, String url) {
        CenterThreadPool.run(() -> {
            // 必须 try-with-resources：code() != 200（短链 302、白名单外 3xx、404/412）时
            // 下面根本不读 body，而 OkHttp 只在 body 读到 EOF 时才把连接还给连接池。
            try (Response response = NetWorkUtil.get(url, NetWorkUtil.webHeaders, location -> handleWebURL(context, location))) {
                ResponseBody body;
                if (response.code() == 200 && (body = response.body()) != null) {
                    JSONObject json = new JSONObject(body.string());
                    if (json.has("code") && json.getInt("code") == -404) {
                        MsgUtil.showMsg(context.getString(R.string.text_list_empty));
                    }
                }
            } catch (IOException | JSONException e) {
                MsgUtil.showMsg(context.getString(R.string.text_parse_failed));
            }
        });
    }

    private static Pair<String, Integer> parseId(String item) {
        Matcher matcher;

        matcher = BV_PATTERN.matcher(item);
        if (matcher.find()) {
            return new Pair<>(matcher.group(), TYPE_BVID);
        }

        matcher = AV_PATTERN.matcher(item);
        if (matcher.find()) {
            return new Pair<>(matcher.group(), TYPE_AVID);
        }

        matcher = CV_PATTERN.matcher(item);
        if (matcher.find()) {
            return new Pair<>(matcher.group(), TYPE_CVID);
        }

        matcher = UID_PATTERN.matcher(item);
        if (matcher.find()) {
            return new Pair<>(matcher.group(), TYPE_UID);
        }

        matcher = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]").matcher(item);
        if (matcher.find()) {
            return new Pair<>(matcher.group(), TYPE_WEB_URL);
        }

        return null;
    }
}
