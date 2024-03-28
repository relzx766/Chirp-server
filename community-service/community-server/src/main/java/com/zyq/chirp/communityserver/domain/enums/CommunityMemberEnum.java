package com.zyq.chirp.communityserver.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 每个角色的code都与 {@link CommunityJoinRangeEnum}相对应
 * 这样可以高效的判断被邀请用户能否加入这个社群
 */
@AllArgsConstructor
@Getter
public enum CommunityMemberEnum {
    MEMBER(2),
    MODERATOR(3),
    ADMIN(4);
    final int code;

    public static CommunityMemberEnum findWithDefault(int code) {
        for (CommunityMemberEnum memberEnum : CommunityMemberEnum.values()) {
            if (memberEnum.code == code) {
                return memberEnum;
            }
        }
        return CommunityMemberEnum.MEMBER;
    }
}
