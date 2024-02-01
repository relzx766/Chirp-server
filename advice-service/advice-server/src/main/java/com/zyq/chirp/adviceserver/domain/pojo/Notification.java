package com.zyq.chirp.adviceserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_site_message")
public class Notification {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String sonEntity;
    private String entity;
    private String entityType;
    private String event;
    private String noticeType;
    private Timestamp createTime;
    private Integer status;


}
