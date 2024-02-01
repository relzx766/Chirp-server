package com.zyq.chirp.authserver.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.authclient.dto.AuthDto;
import com.zyq.chirp.authserver.service.AuthService;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;


@RestController
@RequestMapping("/auth")
public class AuthController {
    @Resource
    UserClient userClient;
    @Resource
    AuthService authService;

    @PostMapping("/signIn")
    public ResponseEntity<AuthDto> signIn(@RequestBody AuthDto authDto) {
        return ResponseEntity.ok(authService.login(authDto));
    }

    @PostMapping("/signUp")
    public ResponseEntity<AuthDto> signUp(@RequestBody UserDto userDto) {
        return ResponseEntity.ok(authService.signUp(userDto));
    }

    @RequestMapping("/signOut")
    public void signOut() {
        StpUtil.logout();
    }

    @GetMapping("/online")
    public ResponseEntity<String> online() {
        authService.online(StpUtil.getLoginIdAsString());
        return ResponseEntity.ok(null);
    }

    @GetMapping("/online/check")
    public ResponseEntity<Boolean> check() {
        return ResponseEntity.ok(authService.getIsOnline(StpUtil.getLoginIdAsString()));
    }

    @PostMapping("/online/check/multi")
    public ResponseEntity<Map<String, Boolean>> multiCheck(@RequestParam("ids") Collection<String> ids) {
        return ResponseEntity.ok(authService.getIsOnline(ids));
    }

    @GetMapping("/offline")
    public ResponseEntity<String> offline() {
        authService.offline(StpUtil.getLoginIdAsString());
        return ResponseEntity.ok(null);
    }
}
