package com.zyq.chirp.common.model;

public enum Code {
    OK(2001),
    ERR(2000),
    UNAUTHORIZED(4003),
    ERR_BUSINESS(5001),
    ERR_SYSTEM(5002);

    private final int code;

    Code(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
