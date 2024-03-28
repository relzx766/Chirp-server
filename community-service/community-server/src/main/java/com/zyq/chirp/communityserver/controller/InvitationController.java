package com.zyq.chirp.communityserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.communityclient.dto.InvitationDto;
import com.zyq.chirp.communityserver.service.InvitationService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/invite")
public class InvitationController {
    @Resource
    InvitationService invitationService;

    @PostMapping("/send")
    public ResponseEntity<Boolean> send(@RequestBody InvitationDto invitationDto) {
        invitationService.send(invitationDto);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/accept")
    public ResponseEntity<Boolean> accept(@RequestBody InvitationDto invitationDto) {
        invitationService.accept(invitationDto);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/reject")
    public ResponseEntity<Boolean> reject(@RequestBody InvitationDto invitationDto) {
        invitationService.reject(invitationDto);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<InvitationDto>> getByInviteeId() {
        return ResponseEntity.ok(invitationService.getByToId(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/fetch")
    public ResponseEntity<List<InvitationDto>> getById(@RequestBody Collection<Long> ids) {
        return ResponseEntity.ok(invitationService.getById(ids));
    }
}
