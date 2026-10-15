package com.RobinNotBad.BiliClient.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class PlayerData implements Parcelable {
    public static int TYPE_VIDEO = 0;
    public static int TYPE_BANGUMI = 1;
    public static int TYPE_LIVE = 2;
    public static int TYPE_LOCAL = 4;

    public String title = "";
    public String videoUrl = "";
    public String danmakuUrl = "";
    public int qn = -1;
    public String[] qnStrList;
    public int[] qnValueList;
    public long aid;
    public long cid;
    public long mid;
    public int progress = 0;
    public long cidHistory = 0;
    // 番剧（PGC）专用字段。投稿视频的观看进度走 x/v2/history/report，只有 aid/cid 两个维度；
    // 番剧必须走心跳接口并带上 epid/sid/sub_type，否则观看记录与续播进度都不会被服务端更新——
    // 表现就是"在终端里看的番剧，历史里不出现，下次还得从头播"。
    public long epid = 0;        // 剧集 epid，就是 Bangumi.Episode#id
    public long seasonId = 0;    // 所在季的 season_id
    public int seasonType = 0;   // 所在季类型（Bangumi.Info#type），作为心跳的 sub_type
    public int type = 0;
    public long timeStamp;
    public ArrayList<String> pagenames;
    public ArrayList<Long> cids;
    /**
     * 这一份 pagenames/cids 是不是"收藏夹虚拟合集"。
     *
     * <p>**必须显式标记，不能靠猜**：两种模式的 cids 语义完全不同——
     * 真实多P 存的是真 cid，收藏夹虚拟合集存的是每个视频的 **aid**。
     * 播放器曾用 `aid != cids[pageIndex]` 来分辨，而真实多P 下 av 号与 cid 永不相等，
     * 于是恒判为虚拟合集：切P 时把 cid 当 aid 去查，`x/web-interface/view` 返回
     * 62002 稿件不可见 → cid 落成 0 → 取流 -400 → **切P 直接失败、播放器卡在原地**
     * （26.10.15 真机复现）。
     */
    public boolean virtualCollection = false;
    /**
     * 正在播放的**真实合集** id（`ugc_season.id`）；0 表示当前播放列表不是合集。
     *
     * <p>用途是"同合集防抖"：自动切合集时，每切一集都会重新查到 `videoInfo.collection`，
     * 若不做这个判断，正在放合集 C 的第 5 集时会又一次"切进 C"，把播放位置重置。
     */
    public long collectionId = 0;
    /** 合集标题。只用于选集卡片上显示"现在在哪一层"，必须单独传：切集后 [title] 会变成剧集标题。 */
    public String collectionTitle = "";
    /**
     * 一层返回栈：从收藏夹连播自动切进合集时，把**收藏夹那条播放列表**存这里，
     * 合集播完后再弹回来继续（否则收藏夹剩下的条目永远轮不到，等于被吞了）。
     *
     * <p>只允许一层——写的时候必须把被存对象的 [returnPlaylist] 置空、[collectionId] 归零，
     * 否则会自我嵌套。
     */
    public PlayerData returnPlaylist = null;
    public int currentPageIndex = 0;
    public DashData dashData; // DASH格式数据
    public String audioUrl = ""; // 单独的音频URL（用于仅音频下载）
    // 直播专用：进入播放器前已经把全部可选地址排好序（见 util/live/LiveStreamSelector）。
    // 直播流"连接还在、但长时间没有数据"时，播放器按这个顺序换 CDN / 换 HLS 重连，
    // 不必回头再请求一次取流接口（也就不会因为请求失败而彻底没得救）。
    public ArrayList<String> liveUrlCandidates = new ArrayList<>();

    public PlayerData() {
    }

    public PlayerData(int type) {
        this.type = type;
    }

    protected PlayerData(Parcel in) {
        title = in.readString();
        videoUrl = in.readString();
        danmakuUrl = in.readString();
        qn = in.readInt();
        qnStrList = in.createStringArray();
        qnValueList = in.createIntArray();
        aid = in.readLong();
        cid = in.readLong();
        mid = in.readLong();
        progress = in.readInt();
        type = in.readInt();
        timeStamp = in.readLong();
        pagenames = in.createStringArrayList();
        cids = new ArrayList<>();
        in.readList(cids, Long.class.getClassLoader());
        currentPageIndex = in.readInt();
        audioUrl = in.readString();
        epid = in.readLong();
        seasonId = in.readLong();
        seasonType = in.readInt();
        liveUrlCandidates = in.createStringArrayList();
        if (liveUrlCandidates == null) liveUrlCandidates = new ArrayList<>();
        virtualCollection = in.readInt() != 0;
        collectionId = in.readLong();
        collectionTitle = in.readString();
        returnPlaylist = in.readParcelable(PlayerData.class.getClassLoader());
        // dashData不序列化，下载时会重新获取
    }

    public static final Creator<PlayerData> CREATOR = new Creator<>() {
        @Override
        public PlayerData createFromParcel(Parcel in) {
            return new PlayerData(in);
        }

        @Override
        public PlayerData[] newArray(int size) {
            return new PlayerData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(videoUrl);
        dest.writeString(danmakuUrl);
        dest.writeInt(qn);
        dest.writeStringArray(qnStrList);
        dest.writeIntArray(qnValueList);
        dest.writeLong(aid);
        dest.writeLong(cid);
        dest.writeLong(mid);
        dest.writeInt(progress);
        dest.writeInt(type);
        dest.writeLong(timeStamp);
        dest.writeStringList(pagenames);
        dest.writeList(cids);
        dest.writeInt(currentPageIndex);
        dest.writeString(audioUrl);
        dest.writeLong(epid);
        dest.writeLong(seasonId);
        dest.writeInt(seasonType);
        dest.writeStringList(liveUrlCandidates);
        dest.writeInt(virtualCollection ? 1 : 0);
        dest.writeLong(collectionId);
        dest.writeString(collectionTitle);
        dest.writeParcelable(returnPlaylist, flags);
        // dashData不序列化，下载时会重新获取
    }

    public boolean isVideo() {
        return type == TYPE_VIDEO;
    }

    public boolean isBangumi() {
        return type == TYPE_BANGUMI;
    }

    public boolean isLive() {
        return type == TYPE_LIVE;
    }

    public boolean isLocal() {
        return type == TYPE_LOCAL;
    }
}
