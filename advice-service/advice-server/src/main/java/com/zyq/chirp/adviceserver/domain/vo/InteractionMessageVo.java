package com.zyq.chirp.adviceserver.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InteractionMessageVo {
    private Long id;

    private Long senderId;
    private String senderAvatar;
    private String senderUsername;

    private Long chirperId;
    private Long receiverId;
    private String text;
    private Timestamp createTime;
    private String type;
}
