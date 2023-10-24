package com.zyq.chirp.adviceserver.util;

public class ChatUtil {
    public static String MathConversationId(Long senderId, Long receiverId) {
        return Math.min(senderId, receiverId) + "_" + Math.max(senderId, receiverId);
    }
}
