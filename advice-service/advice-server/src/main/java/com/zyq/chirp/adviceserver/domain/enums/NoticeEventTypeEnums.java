package com.zyq.chirp.adviceserver.domain.enums;

public enum NoticeEventTypeEnums {
    /**
     * 发布了新的推文，通知关注者
     */
    TWEETED,
    /**
     * 单人私聊
     */
    CHAT,
    /**
     * 点赞
     */
    LIKE,
    /**
     * 转发
     */
    FORWARD,
    /**
     * 引用
     */
    QUOTE,
    /**
     * 回复
     */
    REPLY,
    /**
     * 关注
     */
    FOLLOW,
    /**
     * 提及
     */
    MENTIONED,
    /**
     * 社群邀请
     */
    COMMUNITY_INVITATION,
    /**
     * 公告
     */
    ANNOUNCEMENT,

    /**
     * 通知
     */
    NOTICE;
}
