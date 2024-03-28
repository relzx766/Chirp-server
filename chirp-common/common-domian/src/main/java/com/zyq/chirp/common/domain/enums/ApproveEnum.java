package com.zyq.chirp.common.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ApproveEnum {
    PENDING(1),
    ACCEPTED(2),
    REJECTED(3),
    INVALID(4);
    final int status;

    public static boolean isAvailable(int status) {
        return status == PENDING.status;
    }

    public static ApproveEnum findWithDefault(int status) {
        for (ApproveEnum anEnum : ApproveEnum.values()) {
            if (anEnum.status == status) {
                return anEnum;
            }
        }
        return ACCEPTED;
    }
}
