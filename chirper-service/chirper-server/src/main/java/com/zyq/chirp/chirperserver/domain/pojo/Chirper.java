package com.zyq.chirp.chirperserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@AllArgsConstructor
@Data
@TableName("tb_chirper")
public class Chirper {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long authorId;
    private Long conversationId;
    private Long inReplyToUserId;
    private Long inReplyToChirperId;
    private Timestamp createTime;
    private String text;
    private String type;
    private Long referencedChirperId;
    @TableField(value = "media_keys", typeHandler = JacksonTypeHandler.class)
    private String mediaKeys;
    private Integer viewCount;
    private Integer likeCount;
    private Integer forwardCount;
    private Integer quoteCount;
    private Integer replyCount;
    private Integer status;

    public Chirper() {
        this.createTime = new Timestamp(System.currentTimeMillis());
        this.likeCount = 0;
        this.forwardCount = 0;
        this.quoteCount = 0;
        this.viewCount = 0;
        this.replyCount = 0;
    }
}
