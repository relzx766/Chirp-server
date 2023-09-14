package com.zyq.chirp.userclient.client;

import com.zyq.chirp.userclient.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient("user-server")
public interface UserClient {
    @PostMapping("/user/add")
    ResponseEntity<UserDto> addUser(@RequestBody UserDto userDto);

    @PostMapping("/profile/update")
    ResponseEntity updateProfile(@RequestBody UserDto userDto);

    @GetMapping("/user/profile/{id}")
    ResponseEntity<UserDto> getProfile(@PathVariable("id") Long userId);


    @PostMapping("/user/detail/email")
    ResponseEntity<UserDto> getDetailByEmail(@RequestParam("email") String email);

    @GetMapping("/user/detail/id/{id}")
    ResponseEntity<UserDto> getDetailById(@PathVariable("id") Long id);

    @GetMapping("/user/detail/name/{username}")
    ResponseEntity<UserDto> getDetailByUsername(@PathVariable("username") String username);

    @PostMapping("/user/search/{page}")
    ResponseEntity<List<UserDto>> search(@PathVariable("page") Integer page,
                                         @RequestParam("keyword") String keyword);

    @PostMapping("/rela/follow")
    ResponseEntity follow(@RequestParam("toId") Long toId);

    @PostMapping("/rela/unfollow")
    ResponseEntity unfollow(@RequestParam("toId") Long toId);

    @PostMapping("/rela/block")
    ResponseEntity block(@RequestParam("toId") Long toId);

    @PostMapping("/rela/unblock")
    ResponseEntity unblock(@RequestParam("toId") Long toId);

    @PostMapping("/user/short")
    ResponseEntity<List<UserDto>> getShort(@RequestParam("ids") List<Long> userIds);
}
