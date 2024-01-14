package com.zyq.chirp.userclient.enums;

import lombok.Getter;

/**
 * 需要与userserver里的RelationType同步
 */
@Getter
public enum RelationType {
    /**
     * 关注
     */
    FOLLOWING(1),
    /**
     * 未关注
     */
    UNFOLLOWED(2),
    /**
     * 拉黑
     */
    BLOCK(3);
    private final int relation;

    RelationType(int relation) {
        this.relation = relation;
    }

}
