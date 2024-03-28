package com.zyq.chirp.userserver.model.enumeration;

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

    public int getRelation() {
        return relation;
    }

    public static int findWithDefault(Integer code) {
        if (code == null) {
            return UNFOLLOWED.relation;
        }
        for (RelationType type : RelationType.values()) {
            if (type.getRelation() == code) {
                return type.relation;
            }
        }
        return UNFOLLOWED.relation;
    }
}
