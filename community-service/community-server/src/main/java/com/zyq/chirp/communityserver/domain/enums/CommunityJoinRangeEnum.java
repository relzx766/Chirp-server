package com.zyq.chirp.communityserver.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CommunityJoinRangeEnum {
    /**
     * 所有人都可以加入
     */
    ANYONE(1, "该社群所有人都可以自由加入"),
    /**
     * 成员邀请
     */
    MEMBER(2, "该社群只有被成员邀请才能加入"),
    /**
     * 版主邀请
     */
    MODERATOR(3, "该社群只有被版主邀请才能加入"),
    /**
     * 创建者邀请
     */
    ADMIN(4, "该社群只有被管理员邀请才能加入");
    final int code;
    final String message;

    public static CommunityJoinRangeEnum find(Integer code) {
        if (code == null) {
            return null;
        }
        return find(code.intValue());
    }

    public static CommunityJoinRangeEnum find(int code) {
        for (CommunityJoinRangeEnum rangeEnum : CommunityJoinRangeEnum.values()) {
            if (rangeEnum.getCode() == code) {
                return rangeEnum;
            }
        }
        return null;
    }


}
