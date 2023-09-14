package com.zyq.chirp.authserver.util;

public enum AccountType {
    USERNAME(1),
    EMAIL(2),
    UNKNOWN(3);
    private final int type;

    AccountType(int type) {
        this.type = type;
    }
}