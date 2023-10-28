package com.zyq.chirp.adviceserver.domain.enums;

import lombok.Getter;

@Getter
public enum CacheKey {
    BOUND_CONNECT_INFO("connect"),
    CONVERSATION_KEY("conversation"),
    BOUND_CONVERSATION_USER("conversation:user");
    private final String key;

    CacheKey(String key) {
        this.key = key;
    }
}