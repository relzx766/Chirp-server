package com.zyq.chirp.adviceclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@SuperBuilder
public class NotificationDto extends SiteMessage {
    private String sonEntity;
    private String entity;
    private String entityType;

    private String event;
    private String noticeType;

    private Timestamp createTime;
    private Boolean isRead;
    private Boolean status;

    public NotificationDto() {
        this.isRead = false;
        this.status = true;
        this.createTime = new Timestamp(System.currentTimeMillis());
    }
}
