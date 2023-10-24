package com.zyq.chirp.adviceserver.domain.enums;

import lombok.Getter;

@Getter
public enum ChatStatusEnum {
    /**
     * 撤回等情况（在话题不可见）
     */
    DELETE(0),
    /**
     * 未读
     */
    UNREAD(1),
    /**
     * 已读
     */
    READ(2),
    /**
     * 发送者删除（在发送者视角不可见）
     */
    SENDER_DELETE(3),
    /**
     * 接收者删除（在接收者视角不可见）
     */
    RECEIVER_DELETE(4),
    /**
     * 发送中
     */
    SENDING(5);
    private final int status;

    ChatStatusEnum(int status) {
        this.status = status;
    }
}
