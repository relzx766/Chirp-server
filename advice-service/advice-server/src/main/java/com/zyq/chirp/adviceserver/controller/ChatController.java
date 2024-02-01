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

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Resource
    ChatService chatService;


    @Resource
    MessageAssembleStrategy<ChatDto> assemble;

    @GetMapping("/index")
    public ResponseEntity<List<ChatDto>> getIndexPage() {
        List<ChatDto> messages = chatService.getChatIndex(StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(assemble.assemble(messages));
    }

    @GetMapping("/history/page/{page}/{conversation}")
    public ResponseEntity<List<ChatDto>> getChatHistory(@PathVariable("page") Integer page,
                                                        @PathVariable("conversation") String conversation,
                                                        @RequestParam(defaultValue = "30") Integer size) {
        List<ChatDto> messageDtos = chatService.getChatHistory(conversation, StpUtil.getLoginIdAsLong(), page, size);
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

    @GetMapping("/delete/mark/{id}")
    public ResponseEntity<String> markAsDelete(@PathVariable("id") Long id) {
        chatService.markAsDel(id, StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(null);
    }

    @GetMapping("/leave/{id}")
    public ResponseEntity<String> leaveConversation(@PathVariable("id") String id) {
        chatService.markAsDel(id, StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(null);
    }
}
