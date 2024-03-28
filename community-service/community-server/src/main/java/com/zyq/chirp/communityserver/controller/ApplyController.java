package com.zyq.chirp.communityserver.controller;

import com.zyq.chirp.communityclient.dto.ApplyDto;
import com.zyq.chirp.communityclient.dto.CommunityQueryDto;
import com.zyq.chirp.communityserver.service.ApplyService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apply")
public class ApplyController {
    @Resource
    ApplyService applyService;

    @PostMapping("/approve")
    public ResponseEntity<Boolean> approve(@RequestBody ApplyDto applyDto) {
        applyService.approve(applyDto);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/page")
    public ResponseEntity<List<ApplyDto>> getPage(@RequestBody CommunityQueryDto queryDto) {
        return ResponseEntity.ok(applyService.getPage(queryDto));
    }
}
