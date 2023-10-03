package com.zyq.chirp.feedserver.domain.enums;

import lombok.Getter;

@Getter
public enum CacheKey {
    FEED_BOUND_KEY("feed");
    private final String key;

    CacheKey(String key) {
        this.key = key;
    }

}
