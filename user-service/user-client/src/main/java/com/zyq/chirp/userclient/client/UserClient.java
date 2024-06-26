package com.zyq.chirp.userclient.client;

import com.zyq.chirp.userclient.dto.FollowDto;
import com.zyq.chirp.userclient.dto.RelationDto;
import com.zyq.chirp.userclient.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient("user-server")
public interface UserClient {
    @PostMapping("/user/add")
    ResponseEntity<UserDto> addUser(@RequestBody UserDto userDto);

    @PostMapping("/user/profile/update")
    ResponseEntity<Boolean> updateProfile(@RequestBody UserDto userDto);

    @PostMapping("/user/pwd/update")
    ResponseEntity<Boolean> updatePwd(@RequestBody UserDto userDto);
    @PostMapping("/user/detail/email")
    ResponseEntity<UserDto> getDetailByEmail(@RequestParam("email") String email);

    @GetMapping("/user/detail/id/{id}")
    ResponseEntity<UserDto> getDetailById(@PathVariable("id") Long id);

    @GetMapping("/user/detail/name/{username}")
    ResponseEntity<UserDto> getDetailByUsername(@PathVariable("username") String username);

    @PostMapping("/user/basic_info/rela")
    ResponseEntity<List<UserDto>> getUsernameAndRelation(@RequestParam("userIds") Set<Long> userIds, @RequestParam("id") Long targetId);


    @PostMapping("/user/basic_info")
    ResponseEntity<List<UserDto>> getBasicInfo(@RequestParam("ids") Collection<Long> userIds);

    @PostMapping("/user/id_info")
    ResponseEntity<List<Long>> getIdByUsername(@RequestParam("username") Collection<String> username);

    @GetMapping("/rela/followers/id/{userId}/{page}/{pageSize}")
    ResponseEntity<List<Long>> getFollowerIds(@PathVariable("userId") Long userId,
                                              @PathVariable("page") Integer page,
                                              @PathVariable("pageSize") Integer pageSize);

    @GetMapping("/rela/count/{id}")
    ResponseEntity<FollowDto> getFollowerCount(@PathVariable("id") Long userId);

    @GetMapping("/rela/following/id/{userId}")
    ResponseEntity<List<Long>> getFollowingIds(@PathVariable("userId") Long userId);

    @PostMapping("/rela/people/me/{id}")
    ResponseEntity<List<RelationDto>> getRelationReverseById(@RequestParam("users") Set<Long> userId, @PathVariable("id") Long id);
    @PostMapping("/rela/people/{id}")
    ResponseEntity<List<RelationDto>> getRelation(@RequestParam("users") Set<Long> userId, @PathVariable("id") Long id);

    @PostMapping("/rela/user-relation")
    ResponseEntity<Map<String, RelationDto>> getRelation(@RequestBody Collection<String> fromAndToStrList);
}
