package com.zyq.chirp.feedserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.feedclient.dto.FeedDto;
import com.zyq.chirp.feedserver.service.FeedService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/feed")
public class FeedController {
    @Resource
    FeedService feedService;

    @GetMapping("/{page}")
    public ResponseEntity<Collection<FeedDto>> getPage(@PathVariable("page") Integer page) {
        return ResponseEntity.ok(feedService.getPage(StpUtil.getLoginIdAsString(), page));
    }

    @GetMapping("/score/{score}")
    public ResponseEntity<Collection<FeedDto>> getPageByScore(@PathVariable("score") Double score) {
        return ResponseEntity.ok(feedService.getPageByScore(StpUtil.getLoginIdAsString(), score));
    }
    @GetMapping("/{start}/{end}")
    public ResponseEntity<Collection<FeedDto>> getRange(@PathVariable("start") Double start,
                                                        @PathVariable("end") Double end) {
        return ResponseEntity.ok(feedService.getRange(StpUtil.getLoginIdAsString(), start, end));
    }
}
