package com.zyq.chirp.chirperserver.domain.enums;

public enum CacheKey {
    LIKE_INFO_BOUND_KEY("chirper:like"),
    LIKE_COUNT_BOUND_KEY("count:like"),
    FORWARD_COUNT_BOUND_KEY("count:forward:add"),
    VIEW_COUNT_BOUND_KEY("count:view"),
    FORWARD_INFO_BOUND_KEY("chirper:forward");
    private final String key;

    CacheKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
