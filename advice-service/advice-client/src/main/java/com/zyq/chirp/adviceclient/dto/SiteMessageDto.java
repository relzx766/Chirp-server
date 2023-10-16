package com.zyq.chirp.adviceclient.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@Builder
public class SiteMessageDto {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiverId;
    private String sonEntity;
    private String entity;
    private String entityType;

    private String event;
    private String noticeType;
    private Timestamp createTime;
    private Boolean isRead;
    private Boolean status;
    private Long offset;

    public SiteMessageDto() {
        this.isRead = false;
        this.status = true;
        this.createTime = new Timestamp(System.currentTimeMillis());
    }
}
