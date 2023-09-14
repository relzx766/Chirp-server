package com.zyq.chirp.adviceclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class SiteMessageDto {
    private Long id;

    private Long senderId;
    private String senderAvatar;
    private String senderName;
    /**
     * 推文id等
     */
    private Long targetId;
    private Long receiverId;
    private String text;
    private Timestamp createTime;
    private String type;

    public SiteMessageDto() {
        this.createTime = new Timestamp(System.currentTimeMillis());
    }

    public SiteMessageDto(Long senderId, Long targetId, String type) {
        this.senderId = senderId;
        this.targetId = targetId;
        this.type = type;
        this.createTime = new Timestamp(System.currentTimeMillis());
    }
}
