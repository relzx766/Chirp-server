package com.zyq.chirp.chirperserver.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.chirpclient.dto.LikeDto;
import com.zyq.chirp.chirperserver.service.LikeService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/like")
public class LikeController {
    @Resource
    LikeService likeService;

    @PostMapping("/give")
    public ResponseEntity add(@RequestBody LikeDto likeDto) {
        likeDto.setUserId(StpUtil.getLoginIdAsLong());
        likeService.addLike(likeDto);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/cancel")
    public ResponseEntity cancel(@RequestBody LikeDto likeDto) {
        likeDto.setUserId(StpUtil.getLoginIdAsLong());
        likeService.cancelLike(likeDto);
        return ResponseEntity.ok(null);
    }
}
