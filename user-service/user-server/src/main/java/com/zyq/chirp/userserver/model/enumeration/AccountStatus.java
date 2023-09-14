package com.zyq.chirp.userserver.model.enumeration;

public enum AccountStatus {
    /**
     * 活跃可用
     */
    ACTIVE(1),
    /**
     * 注销
     */
    INACTIVE(2),
    /**
     * 小黑屋
     */
    BLOCK(3);
    private final int status;

    AccountStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
