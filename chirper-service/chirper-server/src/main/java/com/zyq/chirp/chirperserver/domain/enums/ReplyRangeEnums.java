package com.zyq.chirp.chirperserver.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 限制谁可以回复推文
 */
@AllArgsConstructor
@Getter
public enum ReplyRangeEnums {

    EVERYONE(1),
    /**
     * 你关注的
     */
    FOLLOWING(2),
    /**
     * 提及的
     */
    MENTION(3);
    private final int code;

    public static ReplyRangeEnums findByCode(int code) {
        for (ReplyRangeEnums enums : ReplyRangeEnums.values()) {
            if (enums.getCode() == code) {
                return enums;
            }
        }
        return null;
    }

    public static ReplyRangeEnums findByCodeWithDefault(int code) {
        ReplyRangeEnums rangeEnums = findByCode(code);
        return rangeEnums != null ? rangeEnums : ReplyRangeEnums.EVERYONE;
    }

    public static ReplyRangeEnums findByCodeWithDefault(Integer code) {
        if (code == null) {
            return ReplyRangeEnums.EVERYONE;
        }
        return findByCodeWithDefault(code.intValue());
    }
    public static String getHint(int code) {
        return switch (findByCodeWithDefault(code)) {
            case EVERYONE -> "所有人";
            case FOLLOWING -> "关注";
            case MENTION -> "提及";
        };
    }
}
