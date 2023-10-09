package com.zyq.chirp.adviceserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.service.InteractionMessageService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notice")
public class NoticeController {
    @Resource
    InteractionMessageService interactionMessageService;

    @GetMapping("/count/unread")
    public ResponseEntity<Map<String, Integer>> getUnreadCount() {
        Integer count = interactionMessageService.getUnReadCount(StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/page/{page}")
    public ResponseEntity<List<SiteMessageDto>> getPage(@PathVariable("page") Integer page) {
        List<SiteMessageDto> notice = interactionMessageService.getPageByReceiverId(page, StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(interactionMessageService.combine(notice));
    }

    @PostMapping("/read/mark")
    public ResponseEntity<String> markAsRead(@RequestParam("messageId") Collection<Long> messageIds) {
        interactionMessageService.markAsRead(messageIds, StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(null);
    }
}
