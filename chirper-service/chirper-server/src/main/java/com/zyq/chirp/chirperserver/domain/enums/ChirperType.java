package com.zyq.chirp.chirperserver.domain.enums;
//该枚举有在ChirperMapper.xml中使用

public enum ChirperType {
    /**
     * 原创
     */
    ORIGINAL,
    /**
     * 回复
     */
    REPLY,
    /**
     * 转发
     */
    FORWARD,
    /**
     * 引用
     */
    QUOTE;

    public static ChirperType find(String type) {
        for (ChirperType chirperType : ChirperType.values()) {
            if (chirperType.name().equalsIgnoreCase(type)) {
                return chirperType;
            }
        }
        return null;
    }
}
