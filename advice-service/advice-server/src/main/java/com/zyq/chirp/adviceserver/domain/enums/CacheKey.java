package com.zyq.chirp.adviceserver.domain.enums;

import lombok.Getter;

@Getter
public enum CacheKey {
    BOUND_CONNECT_INFO("connect");
    private final String key;

    CacheKey(String key) {
        this.key = key;
    }
}