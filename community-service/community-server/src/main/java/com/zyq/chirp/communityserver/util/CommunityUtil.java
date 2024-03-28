package com.zyq.chirp.communityserver.util;

import com.zyq.chirp.communityserver.domain.enums.CommunityJoinRangeEnum;
import com.zyq.chirp.communityserver.domain.enums.CommunityMemberEnum;

public class CommunityUtil {
    /**
     * 由于{@link CommunityMemberEnum}与{@link CommunityJoinRangeEnum}的设计有对应关系
     */
    public static boolean canJoin(Integer join, Integer member) {
        if (join == null || member == null) {
            return false;
        }
        return member >= join;
    }

    public static boolean canPost(Integer post, Integer member) {
        if (post == null || member == null) {
            return false;
        }
        return member >= post;
    }

}
