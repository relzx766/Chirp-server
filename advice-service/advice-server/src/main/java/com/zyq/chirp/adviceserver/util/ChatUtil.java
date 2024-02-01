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

    /**
     * 判断话题参与者是否都为自己
     *
     * @param conversation 话题
     * @return 话题参与者是否都为自己
     */
    public static boolean isSelfTaking(String conversation) {
        String[] member = splitConversation(conversation);
        return member[0].equals(member[1]);
    }

    /**
     * 话题参与者判断
     *
     * @param conversation 话题
     * @param userId       用户
     * @return 用户是否是该话题的参与者
     */
    public static boolean isParticipant(String conversation, Long userId) {
        String[] participants = splitConversation(conversation);
        String user = userId.toString();
        return participants[0].equals(user) || participants[1].equals(user);
    }
}
