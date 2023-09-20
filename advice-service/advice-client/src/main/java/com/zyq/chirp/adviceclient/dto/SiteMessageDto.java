package com.zyq.chirp.adviceclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class SiteMessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;

    private Long receiverId;
    private String sonEntity;
    private String entity;
    private String entityType;

    private String event;
    private String noticeType;
    private Timestamp createTime;
    private Boolean isRead;
}
