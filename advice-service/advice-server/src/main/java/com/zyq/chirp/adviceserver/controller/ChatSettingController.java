package com.zyq.chirp.adviceserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.adviceclient.dto.ChatSettingDto;
import com.zyq.chirp.adviceserver.service.ChatSettingService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/chat/setting")
public class ChatSettingController {
    @Resource
    ChatSettingService service;

    @GetMapping
    public ResponseEntity<ChatSettingDto> getCurrentUser() {
        return ResponseEntity.ok(service.getCurrentUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatSettingDto> getByUserId(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @PostMapping("/multi")
    public ResponseEntity<Collection<ChatSettingDto>> getByUserIds(@RequestBody Set<Long> userIds) {
        return ResponseEntity.ok(service.getByUserId(userIds));
    }

    @PostMapping("/allow")
    public ResponseEntity<Boolean> updateAllow(@RequestParam("allow") Integer allow) {
        service.updateAllow(StpUtil.getLoginIdAsLong(), allow);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/pinned")
    public ResponseEntity<Boolean> updatePinned(@RequestBody List<String> conversations) {
        service.updatePinned(StpUtil.getLoginIdAsLong(), conversations);
        return ResponseEntity.ok(true);
    }
}
