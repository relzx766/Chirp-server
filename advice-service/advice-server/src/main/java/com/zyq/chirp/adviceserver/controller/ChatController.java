package com.zyq.chirp.adviceserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceserver.service.ChatService;
import com.zyq.chirp.adviceserver.strategy.MessageAssembleStrategy;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/index")
    public ResponseEntity<List<ChatDto>> getIndexPage() {
        long receiverId = StpUtil.getLoginIdAsLong();
        Set<String> conversations = chatService.getConversationByUserId(receiverId);
        List<Long> messageIds = chatService.getConvTop(conversations);
        List<ChatDto> chatDtos = chatService.getById(messageIds);
        assemble.assemble(chatDtos);
        return ResponseEntity.ok(chatDtos);
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
