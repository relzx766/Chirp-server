package com.zyq.chirp.adviceserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zyq.chirp.adviceserver.domain.Message;
import com.zyq.chirp.adviceserver.domain.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@TableName("tb_site_message")
public class InteractionMessage extends Message {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long senderId;


    private String text;

    private Long chirperId;
    private Long receiverId;
    private String type;
    private Timestamp createTime;
    private Boolean isRead;

    public InteractionMessage() {
        this.createTime = new Timestamp(System.currentTimeMillis());
        this.isRead = false;
    }

    public void setType(MessageType messageType) {
        this.type = messageType.toString();
    }

}
