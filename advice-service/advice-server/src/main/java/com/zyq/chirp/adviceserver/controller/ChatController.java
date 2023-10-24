package com.zyq.chirp.adviceserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceserver.service.ChatService;
import com.zyq.chirp.adviceserver.strategy.MessageAssembleStrategy;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Resource
    ChatService chatService;
    @Resource
    MessageAssembleStrategy<ChatDto> assemble;

    @GetMapping("/page/{page}")
    public ResponseEntity<Map<String, Map<String, Object>>> getPage(@PathVariable("page") Integer page) {
        long receiverId = StpUtil.getLoginIdAsLong();
        Set<String> conversations = chatService.getConversationByUserId(receiverId);
        Map<String, List<ChatDto>> chatDtoMap = chatService.getChatByConversation(conversations, page);
        Map<String, Map<String, Object>> data = new HashMap<>();
        Map<String, Integer> unreadCount = chatService.getUnreadCount(conversations, receiverId);
        chatDtoMap.forEach((s, chatDtos) -> {
            assemble.assemble(chatDtos);
            data.put(s, Map.of("data", chatDtos, "unreadCount", unreadCount.get(s)));
        });
        return ResponseEntity.ok(data);
    }

    @GetMapping("/history/page/{page}/{senderId}")
    public ResponseEntity<List<ChatDto>> getChatHistory(@PathVariable("page") Integer page, @PathVariable("senderId") Long senderId) {
        List<ChatDto> messageDtos = chatService.getChatHistory(StpUtil.getLoginIdAsLong(), senderId, page);
        assemble.assemble(messageDtos);
        return ResponseEntity.ok(messageDtos);
    }

    @PostMapping("/unread/get")
    public ResponseEntity<Map<String, Integer>> getUnread(@RequestParam("conversations") List<String> conversations) {
        return ResponseEntity.ok(chatService.getUnreadCount(conversations, StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/read/mark")
    public ResponseEntity<String> markAsRead(@RequestParam("conversations") List<String> conversations) {
        chatService.markAsRead(conversations, StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(null);
    }

}
