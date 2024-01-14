package com.zyq.chirp.chirperserver.domain.enums;

import com.zyq.chirp.common.mq.enums.ActionType;

public enum ActionTypeEnums implements ActionType {
    LIKE,
    FORWARD,
    REPLY,
    QUOTE;

    @Override
    public String getAction() {
        return this.name();
    }
}
