package com.zyq.chirp.chirpclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class ChirperDto {

    private Long id;
    //作者信息--
    private Long authorId;
    private String username;
    private String nickname;
    private String avatar;
    //--
    private Long conversationId;
    private Long inReplyToUserId;
    private Long inReplyToChirperId;

    private Timestamp createTime;
    @Length(max = 500)
    private String text;
    private String type;
    private Long referencedChirperId;
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

    public ChirperDto() {
        this.createTime = new Timestamp(System.currentTimeMillis());
        this.likeCount = 0;
        this.forwardCount = 0;
        this.quoteCount = 0;
        this.viewCount = 0;
        this.replyCount = 0;
        this.isLike = false;
        this.isForward = false;
        this.isQuote = false;
    }
}
