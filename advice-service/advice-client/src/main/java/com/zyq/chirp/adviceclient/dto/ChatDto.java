package com.zyq.chirp.adviceclient.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ChatDto extends SiteMessage {

    @JsonSerialize(using = ToStringSerializer.class)
    /*
      临时id，前端可用此判断消息是否发送成功
     */
    private Long tempId;
    private String conversationId;
    /**
     * 明文的最大长度为1000，密文为1048576，即1M
     */
    private String content;
    private String iv;
    private String type;
    private ChatDto reference;
    private Timestamp createTime;
    private String status;


}
