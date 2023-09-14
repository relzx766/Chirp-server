package com.zyq.chirp.userserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.userclient.dto.UserDto;
import com.zyq.chirp.userserver.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    UserService userService;
    @Resource
    ObjectMapper objectMapper;

    /**
     * 给auth调用
     *
     * @param userDto
     * @return
     */
    @PostMapping("/add")
    public ResponseEntity<UserDto> addUser(@RequestBody @Validated UserDto userDto) {
        return ResponseEntity.ok(userService.save(userDto));
    }

    @PostMapping("/profile/update")
    public ResponseEntity updateProfile(@RequestBody UserDto userDto) throws JsonProcessingException {
        userDto.setId(StpUtil.getLoginIdAsLong());
        userService.update(userDto);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<UserDto> getProfile(@PathVariable("id") Long userId) {
        Long currentUserId = null;
        if (StpUtil.isLogin()) {
            currentUserId = StpUtil.getLoginIdAsLong();
        }
        return ResponseEntity.ok(userService.getById(userId, currentUserId));
    }


    @PostMapping("/detail/email")
    public ResponseEntity<UserDto> getDetailByEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(userService.getDetailByEmail(email));
    }

    @GetMapping("/detail/id/{id}")
    public ResponseEntity<UserDto> getDetailById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getDetailById(id));
    }

    @GetMapping("/detail/name/{username}")
    public ResponseEntity<UserDto> getDetailByUsername(@PathVariable("username") String username) {
        return ResponseEntity.ok(userService.getDetailByUsername(username));
    }

    @PostMapping("/search/{page}")
    public ResponseEntity<List<UserDto>> search(@PathVariable("page") Integer page,
                                                @RequestParam("keyword") String keyword) {
        Long currentUserId = null;
        if (StpUtil.isLogin()) {
            currentUserId = StpUtil.getLoginIdAsLong();
        }
        return ResponseEntity.ok(userService.search(keyword, currentUserId, page));
    }

    @PostMapping("/short")
    public ResponseEntity<List<UserDto>> getShort(@RequestParam("ids") List<Long> userIds) {
        return ResponseEntity.ok(userService.getShortProfile(userIds));
    }
}