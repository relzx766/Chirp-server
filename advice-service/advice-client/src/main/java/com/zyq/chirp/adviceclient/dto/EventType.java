package com.zyq.chirp.adviceclient.dto;

public enum EventType {
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
     * 公告
     */
    ANNOUNCEMENT,
    /**
     * 通知
     */
    NOTICE;
}
