package com.zyq.chirp.common.util;

public class PageUtil {
    public static int getOffset(int page, int size) {
        page = page > 0 ? page : 1;
        return (page - 1) * size;
    }
}
