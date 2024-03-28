package com.zyq.chirp.adviceserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "user_of_chat_public", autoResultMap = true)
public class ChatSetting {
    @TableId(type = IdType.ASSIGN_ID)
    Long id;
    Long userId;
    @TableField("chat_allow")
    Integer allow;
    @TableField(value = "pinned_conversation", typeHandler = JacksonTypeHandler.class)
    List<String> pinned;

}
