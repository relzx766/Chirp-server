package com.zyq.chirp.adviceserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("user_of_chat_public")
public class ChatSetting {
    @TableId(type = IdType.ASSIGN_ID)
    Long id;
    Long userId;
    Integer chatAllow;
    String topConversation;
}
