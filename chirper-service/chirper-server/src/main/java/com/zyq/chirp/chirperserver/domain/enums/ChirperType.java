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
}
