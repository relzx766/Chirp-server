package com.zyq.chirp.adviceserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.adviceclient.dto.E2eeKeypairDto;
import com.zyq.chirp.adviceserver.service.E2EEService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/e2ee")

public class E2EEController {
    @Resource
    E2EEService e2EEService;

    @GetMapping("/pair")
    public ResponseEntity<E2eeKeypairDto> getKeyPair() {
        return ResponseEntity.ok(e2EEService.getKeypair());
    }

    @GetMapping("/key/{id}")
    public ResponseEntity<String> getPublicKey(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(e2EEService.getPublicKey(userId));
    }

    @PostMapping("/keys")
    public ResponseEntity<Map<Long, String>> saveKeys(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(e2EEService.getPublicKey(ids));
    }

    @PostMapping
    public ResponseEntity<String> savePublicKey(@RequestParam("key") String key) {
        e2EEService.savePublicKey(StpUtil.getLoginIdAsLong(), key);
        return ResponseEntity.ok(null);
    }
}
