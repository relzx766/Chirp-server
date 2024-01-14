package com.zyq.chirp.common.mq.enums;

public enum DefaultOperation implements Operation {
    INCREMENT,
    DECREMENT,
    INSET,
    DELETE;


    @Override
    public String getOperation() {
        return this.name();
    }
}
