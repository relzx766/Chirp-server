package com.zyq.chirp.userserver.model.enumeration;

import lombok.Getter;

@Getter
public enum CacheKey {
    FOLLOW_COUNT_BOUND_KEY("user:follow");;
    private final String key;

    CacheKey(String key) {
        this.key = key;
    }
}
