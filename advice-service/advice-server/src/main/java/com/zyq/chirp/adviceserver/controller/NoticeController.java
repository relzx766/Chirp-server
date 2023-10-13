package com.zyq.chirp.adviceserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.adviceclient.dto.ReadRecordDto;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.service.InteractionMessageService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/read/mark")
    public ResponseEntity<ReadRecordDto> markAsRead() {
        interactionMessageService.readAll(StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(null);
    }
}
