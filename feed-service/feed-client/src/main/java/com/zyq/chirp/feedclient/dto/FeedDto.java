package com.zyq.chirp.feedclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FeedDto {
    /**
     * 接收者
     */
    String receiverId;
    /**
     * 发布者
     */
    String publisher;
    /**
     * 内容id
     */
    String contentId;
    /**
     * 分数，表现为时间
     */
    Double score;
}
