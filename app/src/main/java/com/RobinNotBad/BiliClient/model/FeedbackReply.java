package com.RobinNotBad.BiliClient.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * 反馈对话里的一条消息（26.10.16 新增）。
 *
 * <p>一条反馈下面可能有**两侧**的消息，服务端把它们按时间合并成一条时间线后一起返回
 * （见服务端 {@code _db_feedback_thread}），靠 {@link #author} 区分：
 * <ul>
 *   <li>{@code "dev"}  —— 开发者回复。服务端把它写成一条定向公告
 *       （{@code announcements.reply_feedback_id} 关联），所以它有 {@link #title}；</li>
 *   <li>{@code "user"} —— 用户自己追加的说明（{@code feedback_comment} 表），没有标题。</li>
 * </ul>
 *
 * <p>实现 {@link Parcelable} 是为了让列表页能把整条反馈（含消息列表）直接传给详情页，
 * 免去详情页再发一次网络请求。
 */
public class FeedbackReply implements Parcelable {
    public int id;
    /** 开发者回复的标题；用户自己的消息没有标题，是空串。 */
    public String title;
    public String content;
    /** 已格式化的可读时间（服务端返回的是 epoch 秒，解析时已换算）。 */
    public String ctime;
    /**
     * 谁写的：{@code "dev"} / {@code "user"}。见类注释。
     *
     * <p>**旧服务端不返回这个字段**（那时只有开发者回复这一侧），缺失时按空串存，
     * 于是 {@link #isFromUser()} 为 false —— 正好等于升级前的唯一可能，
     * 所以不需要额外的兼容分支。
     */
    public String author;

    public FeedbackReply(int id, String title, String content, String ctime, String author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.ctime = ctime;
        this.author = author;
    }

    /** 这条是不是用户自己写的（决定详情页用哪套样式与标签）。 */
    public boolean isFromUser() {
        return AUTHOR_USER.equals(author);
    }

    /** {@link #author} 的取值。与服务端 `_db_feedback_thread` 里的字面量必须一致。 */
    public static final String AUTHOR_DEV = "dev";
    public static final String AUTHOR_USER = "user";

    protected FeedbackReply(Parcel in) {
        id = in.readInt();
        title = in.readString();
        content = in.readString();
        ctime = in.readString();
        author = in.readString();
    }

    public static final Creator<FeedbackReply> CREATOR = new Creator<FeedbackReply>() {
        @Override
        public FeedbackReply createFromParcel(Parcel in) {
            return new FeedbackReply(in);
        }

        @Override
        public FeedbackReply[] newArray(int size) {
            return new FeedbackReply[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(ctime);
        dest.writeString(author);
    }
}
