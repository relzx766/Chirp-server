package com.zyq.chirp.common.model.enumration;

public enum SearchAction {
    TIME,
    RANDOM,
    HOT;

    public static SearchAction find(String order) {
        for (SearchAction searchAction : SearchAction.values()) {
            if (searchAction.name().equals(order)) {
                return searchAction;
            }
        }
        return null;
    }
}
