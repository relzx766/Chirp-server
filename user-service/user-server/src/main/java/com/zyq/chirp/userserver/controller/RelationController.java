package com.zyq.chirp.userserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.userserver.service.RelationService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/followers/id/{userId}/{page}/{pageSize}")
    public ResponseEntity<List<Long>> getFollowerIds(@PathVariable("userId") Long userId,
                                                     @PathVariable("page") Integer page,
                                                     @PathVariable("pageSize") Integer pageSize) {
        return ResponseEntity.ok(relationService.getFollower(userId, page, pageSize));
    }

    @GetMapping("/count/{id}")
    public ResponseEntity<Long> getFollowerCount(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(relationService.getFollowerCount(userId));
    }
}
