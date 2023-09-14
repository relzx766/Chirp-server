package com.zyq.chirp.chirperserver.domain.enums;

//该枚举有在ChirperMapper.xml中使用
public enum ChirperStatus {
    ACTIVE(1),
    DELETE(2),
    /**
     * 定时发布，该状态下的推文仅作者以及具有相应权限角色可查看
     */
    DELAY(3);
    private final int status;

    ChirperStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
