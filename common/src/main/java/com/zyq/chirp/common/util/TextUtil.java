package com.zyq.chirp.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {
    public static List<String> findMentioned(String text) {
        Pattern pattern = Pattern.compile("@[^@\\s]+");
        Matcher matcher = pattern.matcher(text);
        List<String> str = new ArrayList<>();
        while (matcher.find()) {
            str.add(matcher.group().replace("@", ""));
        }
        return str;
    }
}
