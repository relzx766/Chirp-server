package com.zyq.chirp.chirpclient.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@Builder
public class ChirperDto {

    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)

    private Long id;
    //作者信息--
    private Long authorId;
    private String username;
    private String nickname;
    private String avatar;
    //--
    private Long conversationId;
    private Long inReplyToUserId;
    @NotNull(groups = Reply.class, message = "回复对象不能为空")
    private Long inReplyToChirperId;

    private Timestamp createTime;
    @Length(max = 500)
    private String text;
    private String type;
    @NotNull(groups = Quote.class, message = "引用对象不能为空")
    private Long referencedChirperId;
    private ChirperDto referenced;
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

    public interface Reply {
    }

    public interface Quote {
    }
}
