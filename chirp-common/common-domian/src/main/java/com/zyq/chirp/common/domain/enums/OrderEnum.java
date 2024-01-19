package com.zyq.chirp.common.domain.enums;

public enum OrderEnum {
    HOT,
    ASC,
    DESC,
    ;

    public static OrderEnum findAndDefault(String order) {
        for (OrderEnum orderEnum : OrderEnum.values()) {
            if (orderEnum.name().equalsIgnoreCase(order)) {
                return orderEnum;
            }
        }
        return OrderEnum.DESC;
    }
}
