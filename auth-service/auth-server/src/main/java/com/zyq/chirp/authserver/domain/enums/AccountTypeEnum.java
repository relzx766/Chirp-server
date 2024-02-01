package com.zyq.chirp.authserver.domain.enums;

public enum AccountTypeEnum {
    USERNAME,
    EMAIL;

    public static AccountTypeEnum findWithDefault(String type) {
        for (AccountTypeEnum typeEnum : AccountTypeEnum.values()) {
            if (typeEnum.name().equalsIgnoreCase(type)) {
                return typeEnum;
            }
        }
        return AccountTypeEnum.USERNAME;
    }
}
