package com.zyq.chirp.common.util;

public class PageUtil {
    public static int getOffset(int page, int size) {
        page = Math.max(page, 1);
        return (page - 1) * size;
    }
}
