package com.zyq.chirp.userserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.userserver.service.RelationService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rela")
public class RelationController {
    @Resource
    RelationService relationService;

    @PostMapping("/follow")
    public ResponseEntity follow(@RequestParam("toId") Long toId) {
        relationService.follow(StpUtil.getLoginIdAsLong(), toId);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/unfollow")
    public ResponseEntity unfollow(@RequestParam("toId") Long toId) {
        relationService.unfollow(StpUtil.getLoginIdAsLong(), toId);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/block")
    public ResponseEntity block(@RequestParam("toId") Long toId) {
        relationService.block(StpUtil.getLoginIdAsLong(), toId);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/unblock")
    public ResponseEntity unblock(@RequestParam("toId") Long toId) {
        relationService.cancelBlock(StpUtil.getLoginIdAsLong(), toId);
        return ResponseEntity.ok(null);
    }
}
