package com.zyq.chirp.communityserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.communityclient.dto.MemberDto;
import com.zyq.chirp.communityclient.dto.CommunityQueryDto;
import com.zyq.chirp.communityserver.service.MemberService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/member")
public class MemberController {
    @Resource
    MemberService memberService;

    @PostMapping("/query")
    public ResponseEntity<List<MemberDto>> getMember(@RequestBody CommunityQueryDto communityQueryDto) {
        return ResponseEntity.ok(memberService.getPage(communityQueryDto));
    }

    @GetMapping("/{communityId}")
    public ResponseEntity<MemberDto> getOne(@PathVariable("communityId") Long communityId) {
        return ResponseEntity.ok(memberService.getOne(StpUtil.getLoginIdAsLong(), communityId));
    }

}
