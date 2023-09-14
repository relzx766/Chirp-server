package com.zyq.chirp.chirpclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * 用于给用户推送点赞消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeDto {
    /**
     * 点赞人
     */
    private Long userId;

    /**
     * 被点赞推文
     */
    private Long chirperId;

    /**
     * 点赞还是取消
     */
    private Integer type;
    private Timestamp createTime;
}
