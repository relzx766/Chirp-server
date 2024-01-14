package com.zyq.chirp.adviceserver.domain.enums;

public enum ChatAllowEnum {
    NOBODY,
    EVERYONE;

    public static ChatAllowEnum find(String str) {
        try {
            return ChatAllowEnum.valueOf(ChatAllowEnum.class, str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static ChatAllowEnum find(int ordinal) {
        return ChatAllowEnum.values()[ordinal];
    }
}
