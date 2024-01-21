package com.zyq.chirp.chirperserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.zyq.chirp.chirperserver.domain.enums.ReplyRangeEnums;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@AllArgsConstructor
@Data
@Builder
@TableName(value = "tb_chirper", autoResultMap = true)
public class Chirper {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long authorId;
    private Long conversationId;
    private Long inReplyToUserId;
    private Long inReplyToChirperId;
    private Timestamp createTime;
    /**
     * 什么时候激活
     */
    private Timestamp activeTime;
    private String text;
    private String type;
    private Long referencedChirperId;
    @TableField(value = "media_keys", typeHandler = JacksonTypeHandler.class)
    private List<Integer> mediaKeys;
    private Integer viewCount = 0;
    private Integer likeCount = 0;
    private Integer forwardCount = 0;
    private Integer quoteCount = 0;
    private Integer replyCount = 0;
    private Integer replyRange = ReplyRangeEnums.EVERYONE.getCode();
    private Integer status;

    public Chirper() {
        this.createTime = new Timestamp(System.currentTimeMillis());
    }
}
