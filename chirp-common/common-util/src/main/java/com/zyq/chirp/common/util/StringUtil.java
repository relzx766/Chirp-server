package com.zyq.chirp.common.util;

import java.util.Arrays;

public class StringUtil {
    private static final String separator = ":";

    public static String combineKey(Object... keys) {
        return String.join(separator, Arrays.stream(keys).map(String::valueOf).toList());
    }

    public static String combineKey(String... keys) {
        return String.join(separator, keys);
    }
    public static String[] divideKey(String key) {
        return key.split(separator);
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
