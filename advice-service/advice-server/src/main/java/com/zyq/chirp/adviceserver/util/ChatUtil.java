package com.zyq.chirp.adviceserver.util;

public class ChatUtil {
    public static final String SEPARATOR = "_";

    public static String mathConversationId(Long senderId, Long receiverId) {
        return Math.min(senderId, receiverId) + SEPARATOR + Math.max(senderId, receiverId);
    }

    /**
     * 分离话题，提取出话题参与者
     *
     * @param conversation 以"_"为分隔符
     * @return 参与者id数组
     */
    public static String[] splitConversation(String conversation) {
        return conversation.split(SEPARATOR);
    }
}
