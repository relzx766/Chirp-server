package com.zyq.chirp.userserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.userclient.dto.UserDto;
import com.zyq.chirp.userserver.service.RelationService;
import com.zyq.chirp.userserver.service.UserService;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    UserService userService;
    @Resource
    ObjectMapper objectMapper;
    @Resource
    RelationService relationService;

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

    @GetMapping("/profile/{account}")
    public ResponseEntity<UserDto> getProfile(@PathVariable("account") String account,
                                              @Nullable @RequestParam("type") String type) {
        Long currentUserId = null;
        if (StpUtil.isLogin()) {
            currentUserId = StpUtil.getLoginIdAsLong();
        }

        if (type != null) {
            return ResponseEntity.ok(userService.getByUsername(account, currentUserId));
        }
        return ResponseEntity.ok(userService.getById(Long.parseLong(account), currentUserId));
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

    @GetMapping("/search/{page}")
    public ResponseEntity<List<UserDto>> search(@PathVariable("page") Integer page,
                                                @RequestParam("keyword") String keyword) {
        Long currentUserId = null;
        if (StpUtil.isLogin()) {
            currentUserId = StpUtil.getLoginIdAsLong();
        }
        return ResponseEntity.ok(userService.search(keyword, currentUserId, page));
    }

    @PostMapping("/basic_info")
    public ResponseEntity<List<UserDto>> getBasicInfo(@RequestParam("ids") Collection<Long> userIds) {
        return ResponseEntity.ok(userService.getBasicInfo(userIds));
    }

    @PostMapping("/id_info")
    public ResponseEntity<List<Long>> getIdByUsername(@RequestParam("username") Collection<String> username) {
        return ResponseEntity.ok(userService.getIdByUsername(username));
    }

    @GetMapping("/follower/{id}/{page}")
    public ResponseEntity<List<UserDto>> getFollower(@PathVariable("id") Long id,
                                                     @PathVariable("page") Integer page) {
        int pageSize = 40;
        List<Long> follower = relationService.getFollower(id, page, pageSize);
        if (StpUtil.isLogin()) {
            return ResponseEntity.ok(userService.getByIds(follower, StpUtil.getLoginIdAsLong()));
        }
        return ResponseEntity.ok(userService.getByIds(follower, null));
    }

    @GetMapping("/following/{id}/{page}")
    public ResponseEntity<List<UserDto>> getFollowing(@PathVariable("id") Long id,
                                                      @PathVariable("page") Integer page) {
        int pageSize = 40;
        List<Long> following = relationService.getFollowing(id, page, pageSize);
        if (StpUtil.isLogin()) {
            return ResponseEntity.ok(userService.getByIds(following, StpUtil.getLoginIdAsLong()));
        }
        return ResponseEntity.ok(userService.getByIds(following, null));
    }


}
