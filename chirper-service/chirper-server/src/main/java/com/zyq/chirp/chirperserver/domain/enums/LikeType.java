package com.zyq.chirp.chirperserver.domain.enums;

public enum LikeType {
    ADD(1),
    CANCEL(0);
    private final int type;

    LikeType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
