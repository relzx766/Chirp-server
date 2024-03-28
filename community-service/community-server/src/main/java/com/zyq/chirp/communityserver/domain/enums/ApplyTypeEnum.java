package com.zyq.chirp.communityserver.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplyTypeEnum {
    JOIN(1);
    final int code;

    public static ApplyTypeEnum findWithDefault(int code) {
        for (ApplyTypeEnum typeEnum : ApplyTypeEnum.values()) {
            if (typeEnum.code == code) {
                return typeEnum;
            }
        }
        return JOIN;
    }
}
