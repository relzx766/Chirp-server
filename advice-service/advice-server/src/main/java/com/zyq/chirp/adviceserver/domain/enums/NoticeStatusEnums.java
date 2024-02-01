package com.zyq.chirp.adviceserver.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoticeStatusEnums {
    UNREAD(1),
    READ(2),
    DELETE(3);
    private final int status;
}
