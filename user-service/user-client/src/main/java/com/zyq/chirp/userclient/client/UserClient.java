package com.zyq.chirp.userclient.client;

import com.zyq.chirp.userclient.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@FeignClient("user-server")
public interface UserClient {
    @PostMapping("/user/add")
    ResponseEntity<UserDto> addUser(@RequestBody UserDto userDto);

    @PostMapping("/user/detail/email")
    ResponseEntity<UserDto> getDetailByEmail(@RequestParam("email") String email);

    @GetMapping("/user/detail/id/{id}")
    ResponseEntity<UserDto> getDetailById(@PathVariable("id") Long id);

    @GetMapping("/user/detail/name/{username}")
    ResponseEntity<UserDto> getDetailByUsername(@PathVariable("username") String username);


    @PostMapping("/user/basic_info")
    ResponseEntity<List<UserDto>> getShort(@RequestParam("ids") List<Long> userIds);

    @PostMapping("/user/id_info")
    ResponseEntity<List<Long>> getIdByUsername(@RequestParam("username") Collection<String> username);

    @GetMapping("/rela/followers/id/{userId}/{page}/{pageSize}")
    ResponseEntity<List<Long>> getFollowerIds(@PathVariable("userId") Long userId,
                                              @PathVariable("page") Integer page,
                                              @PathVariable("pageSize") Integer pageSize);

    @GetMapping("/rela/count/{id}")
    ResponseEntity<Long> getFollowerCount(@PathVariable("id") Long userId);
}
