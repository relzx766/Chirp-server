package com.zyq.chirp.chirperserver.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChirperVo {
    private Long id;
    private Long authorId;
    private Long conversationId;
    private Long inReplyToUserId;
    private Timestamp createTime;
    private String text;
    private String type;
    private Long referencedChirperId;
    private Long inReplyToChirperId;

    private String mediaKeys;
    private Integer viewCount;
    private Integer likeCount;
    private Integer forwardCount;
    private Integer quoteCount;
    private Integer replyCount;
    private Boolean isLike;
    private Boolean isForward;
    private Boolean isQuote;
    private Integer status;
}
