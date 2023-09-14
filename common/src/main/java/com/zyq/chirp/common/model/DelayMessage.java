package com.zyq.chirp.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;

@Data
@AllArgsConstructor
public class DelayMessage {
    /**
     * 消息数据
     */
    Object data;
    /**
     * 延时时间
     */
    Duration delayTime;
    /**
     * 发送时间
     */
    Long createTime;

    public DelayMessage() {
        this.createTime = System.currentTimeMillis();
    }

    public DelayMessage(Object data, Duration delayTime) {
        this.data = data;
        this.delayTime = delayTime;
        this.createTime = System.currentTimeMillis();
    }
}
