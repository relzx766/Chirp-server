package com.zyq.chirp.common.mq;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Message<T> {
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    private T body;
    private Integer retryTimes;

    public Message() {
        this.retryTimes = 0;
    }
}
