package com.zyq.chirp.adviceserver.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class SiteMessage {
    private Long sender;
    private String senderAvatar;
    private String senderUsername;
    /**
     * 推文id等
     */
    private Long targetId;
    private Long receiver;
    private String text;
    private Timestamp createTime;
    private String type;

    public SiteMessage() {
        this.createTime = new Timestamp(System.currentTimeMillis());
    }

    public void setType(MessageType messageType) {
        this.type = messageType.toString();
    }
}
