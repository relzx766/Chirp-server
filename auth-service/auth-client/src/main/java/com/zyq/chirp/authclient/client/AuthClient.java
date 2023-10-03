package com.zyq.chirp.authclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.Map;

@FeignClient("auth-server")
public interface AuthClient {
    @GetMapping("/auth/online")
    ResponseEntity<String> online();

    @GetMapping("/auth/online/check")
    ResponseEntity<Boolean> check();

    @GetMapping("/auth/offline")
    ResponseEntity<String> offline();

    @PostMapping("/auth/online/check/multi")
    ResponseEntity<Map<String, Boolean>> multiCheck(@RequestParam("ids") Collection<String> ids);
}
