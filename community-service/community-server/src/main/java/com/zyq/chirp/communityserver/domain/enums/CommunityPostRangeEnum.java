package com.zyq.chirp.communityserver.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 每个code都与 {@link CommunityMemberEnum}相对应
 * 这样可以高效的判断用户能否在社群发布、评论、点赞
 */
@AllArgsConstructor
@Getter
public enum CommunityPostRangeEnum {
    ANYONE(2),
    Moderator(3),
    ADMIN(4);
    final int code;

    public static CommunityPostRangeEnum find(int code) {
        for (CommunityPostRangeEnum rangeEnum : CommunityPostRangeEnum.values()) {
            if (rangeEnum.getCode() == code) {
                return rangeEnum;
            }
        }
        return null;
    }

}
