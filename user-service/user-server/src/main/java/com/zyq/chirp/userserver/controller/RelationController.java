package com.zyq.chirp.userserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.userclient.dto.FollowDto;
import com.zyq.chirp.userclient.dto.RelationDto;
import com.zyq.chirp.userserver.service.RelationService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    //获取他人与我的关系
    @PostMapping("/people/me")
    public ResponseEntity<List<RelationDto>> getRelationReverse(@RequestBody Set<Long> userId) {
        return ResponseEntity.ok(relationService.getUserRelationReverse(userId, StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/people/me/{id}")
    public ResponseEntity<List<RelationDto>> getRelationReverseById(@RequestParam("users") Set<Long> userId, @PathVariable("id") Long id) {
        return ResponseEntity.ok(relationService.getUserRelationReverse(userId, id));
    }
    @PostMapping("/people/{id}")
    public ResponseEntity<List<RelationDto>> getRelation(@RequestParam("users") Set<Long> userId, @PathVariable("id") Long id) {
        return ResponseEntity.ok(relationService.getUserRelation(userId, id));
    }
    @GetMapping("/followers/id/{userId}/{page}/{pageSize}")
    public ResponseEntity<List<Long>> getFollowerIds(@PathVariable("userId") Long userId,
                                                     @PathVariable("page") Integer page,
                                                     @PathVariable("pageSize") Integer pageSize) {
        return ResponseEntity.ok(relationService.getFollower(userId, page, pageSize));
    }

    @GetMapping("/following/id/{userId}")
    public ResponseEntity<List<Long>> getFollowingIds(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(relationService.getFollowing(userId));
    }

    @GetMapping("/count/{id}")
    public ResponseEntity<FollowDto> getFollowerCount(@PathVariable("id") Long userId) {
        FollowDto followDto = new FollowDto();
        followDto.setFollower(relationService.getFollowerCount(userId));
        followDto.setFollowing(relationService.getFollowingCount(userId));
        return ResponseEntity.ok(followDto);
    }

    @PostMapping("/user-relation")
    public ResponseEntity<Map<String, RelationDto>> getRelation(@RequestBody Collection<String> fromAndToStrList) {
        return ResponseEntity.ok(relationService.getUserRelation(fromAndToStrList));
    }
}
