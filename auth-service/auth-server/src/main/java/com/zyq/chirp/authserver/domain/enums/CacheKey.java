package com.zyq.chirp.authserver.domain.enums;

import lombok.Getter;

@Getter
public enum CacheKey {
    BOUND_ONLINE_INFO("online");
    private final String key;

    CacheKey(String key) {
        this.key = key;
    }
}
