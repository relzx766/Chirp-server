package com.zyq.chirp.adviceclient.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class SiteMessage {
    @JsonSerialize(using = ToStringSerializer.class)
    protected Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    protected Long senderId;
    protected String senderName;
    protected String senderAvatar;
    @JsonSerialize(using = ToStringSerializer.class)
    protected Long receiverId;
    protected String receiverName;
    protected String receiverAvatar;
    protected String failedMsg;
}
