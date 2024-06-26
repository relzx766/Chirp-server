package com.zyq.chirp.chirpclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.sql.Timestamp;
import java.util.List;

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
    private Long communityId;
    private Long inReplyToUserId;
    @NotNull(groups = Reply.class, message = "回复对象不能为空")
    private Long inReplyToChirperId;

    private Timestamp createTime;
    private Timestamp activeTime;
    @Length(max = 1000, message = "最大字数为1000")
    private String text;
    private String type;
    @NotNull(groups = Quote.class, message = "引用对象不能为空")
    private Long referencedChirperId;
    private ChirperDto referenced;
    @Size(max = 4, message = "最多支持4个媒体文件")
    private List<MediaDto> mediaKeys;
    private Integer viewCount;
    private Integer likeCount;
    private Integer forwardCount;
    private Integer quoteCount;
    private Integer replyCount;
    private Boolean isLike;
    private Boolean isForward;
    private Boolean isQuote;
    private Integer replyRange;
    private Boolean replyable = false;
    private Boolean forwardable = false;
    private Boolean quotable = false;
    private Boolean likeable = false;
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

    @JsonIgnore
    public boolean isEmpty() {
        return (text == null || text.trim().isEmpty()) && (mediaKeys == null || mediaKeys.isEmpty());
    }

    public void setAllInteractionAllow() {
        this.setReplyable(true);
        this.setLikeable(true);
        this.setQuotable(true);
        this.setForwardable(true);
    }

    public void setAllInteractionDeny() {
        this.setReplyable(false);
        this.setLikeable(false);
        this.setQuotable(false);
        this.setForwardable(false);
    }
}
