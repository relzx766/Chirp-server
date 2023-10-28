package com.zyq.chirp.adviceserver.domain.enums;

public enum ChatTypeEnum {
    TEXT,
    IMAGE,
    VOICE,
    FILE;

    public static ChatTypeEnum getEnum(String type) {
        if (type != null) {
            for (ChatTypeEnum chatTypeEnum : ChatTypeEnum.values()) {
                if (chatTypeEnum.name().equalsIgnoreCase(type)) {
                    return chatTypeEnum;
                }
            }
        }
        return null;
    }
}
