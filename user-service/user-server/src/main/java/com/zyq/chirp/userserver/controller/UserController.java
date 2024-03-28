package com.zyq.chirp.userserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.userclient.dto.UserDto;
import com.zyq.chirp.userserver.service.RelationService;
import com.zyq.chirp.userserver.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    UserService userService;
    @Resource
    ObjectMapper objectMapper;
    @Resource
    RelationService relationService;

    @PostMapping("/add")
    public ResponseEntity<UserDto> addUser(@RequestBody @Validated UserDto userDto) {
        return ResponseEntity.ok(userService.save(userDto));
    }

    @PostMapping("/profile")
    public ResponseEntity<List<UserDto>> getProfiles(@RequestBody List<Long> userIds) {
        return ResponseEntity.ok(userService.getByIds(userIds, StpUtil.getLoginIdAsLong()));
    }
    @PostMapping("/profile/update")
    public ResponseEntity<Boolean> updateProfile(@RequestBody UserDto userDto) {
        userDto.setId(StpUtil.getLoginIdAsLong());
        userService.update(userDto);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/pwd/update")
    public ResponseEntity<Boolean> updatePwd(@RequestBody UserDto userDto) {
        userService.update(userDto);
        return ResponseEntity.ok(true);
    }
    @GetMapping("/load")
    public ResponseEntity<UserDto> loadUser() {
        return ResponseEntity.ok(userService.getByIds(List.of(StpUtil.getLoginIdAsLong()), null).getFirst());
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<UserDto> getProfile(@PathVariable("username") String username) {
        Long currentUserId = null;
        if (StpUtil.isLogin()) {
            currentUserId = StpUtil.getLoginIdAsLong();
        }
        return ResponseEntity.ok(userService.getByUsername(username, currentUserId));
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

    @GetMapping("/username/check/{username}")
    public ResponseEntity<Boolean> checkUsername(@PathVariable("username") String username) {
        return ResponseEntity.ok(userService.isUnExist(username));
    }

    @GetMapping("/email/check/{email}")
    public ResponseEntity<Boolean> checkEmail(@PathVariable("email") String email) {
        return ResponseEntity.ok(userService.isEmailExist(email));
    }

    @PostMapping("/basic_info/rela")
    public ResponseEntity<List<UserDto>> getUsernameAndRelation(@RequestParam("userIds") Set<Long> userIds, @RequestParam("id") Long targetId) {
        return ResponseEntity.ok(userService.getBasicInfo(userIds, targetId));
    }
}
