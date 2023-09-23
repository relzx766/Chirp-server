package com.zyq.chirp.chirperserver.domain.enums;

public enum SearchOrder {
    TIME,
    HOT;

    public static SearchOrder find(String order) {
        for (SearchOrder searchOrder : SearchOrder.values()) {
            if (searchOrder.name().equals(order)) {
                return searchOrder;
            }
        }
        return null;
    }
}
