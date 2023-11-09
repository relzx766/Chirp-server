package com.zyq.chirp.adviceserver.domain.enums;


public enum ChatStatusEnum {
    /**
     * 撤回，双方都删除等情况（在话题不可见）
     */
    DELETE,
    /**
     * 未读
     */
    UNREAD,
    /**
     * 已读
     */
    READ,
    /**
     * 发送中
     */
    SENDING;
}
