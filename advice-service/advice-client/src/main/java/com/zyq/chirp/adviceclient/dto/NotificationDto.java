package com.zyq.chirp.adviceclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class NotificationDto extends SiteMessage {
    private String sonEntity;
    private String entity;
    private String entityType;
    private String event;
    private String noticeType;
    private Timestamp createTime = new Timestamp(System.currentTimeMillis());
    private Integer status;


}
