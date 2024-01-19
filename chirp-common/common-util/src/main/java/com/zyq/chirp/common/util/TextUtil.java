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

    public static boolean getIsMentioned(String text, String username) {
        Pattern pattern = Pattern.compile(STR."@\{username}\\b");
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    public static List<String> findTags(String text) {
        Pattern pattern = Pattern.compile("#[^#\\s]+");
        Matcher matcher = pattern.matcher(text);
        List<String> str = new ArrayList<>();
        while (matcher.find()) {
            str.add(matcher.group());
        }
        return str;
    }

    public static List<String> findTags(List<String> text) {
        Pattern pattern = Pattern.compile("#[^#\\s]+");
        List<String> str = new ArrayList<>();
        text.forEach(content -> {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                str.add(matcher.group());
            }
        });
        return str;
    }

}
