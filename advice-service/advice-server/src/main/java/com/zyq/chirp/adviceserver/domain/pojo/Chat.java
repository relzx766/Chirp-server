package com.zyq.chirp.adviceserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("tb_private_message")
public class Chat {
    @TableId(type = IdType.ASSIGN_ID)
    Long id;
    Long senderId;
    Long receiverId;
    String conversationId;
    String content;
    String type;
    Timestamp createTime;
    Integer status;
}
