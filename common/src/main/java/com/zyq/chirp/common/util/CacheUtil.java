package com.zyq.chirp.common.util;

import java.util.Arrays;

public class CacheUtil {
    private static final String separator = ":";

    public static String combineKey(Object... keys) {
        return String.join(separator, Arrays.stream(keys).map(String::valueOf).toList());
    }

    public static String[] divideKey(String key) {
        return key.split(separator);
    }
}
