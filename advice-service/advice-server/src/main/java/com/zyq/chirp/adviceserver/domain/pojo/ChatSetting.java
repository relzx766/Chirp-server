package com.zyq.chirp.adviceserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_of_chat_public")
public class ChatSetting {
    @TableId(type = IdType.ASSIGN_ID)
    Long id;
    Long userId;
    @TableField("chat_allow")
    Integer allow;
    @TableField("pinned_conversation")
    String pinned;
}
