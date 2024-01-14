package com.zyq.chirp.chirpclient.client;

import com.zyq.chirp.chirpclient.dto.ChirperDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@FeignClient("chirper-server")
public interface ChirperClient {
    //chirperController

    @PostMapping("/chirper/content")
    ResponseEntity<List<ChirperDto>> getContent(@RequestParam("ids") List<Long> ids);

    @GetMapping("/following/{id}/{size}")
    ResponseEntity<List<ChirperDto>> getByFollowerId(@PathVariable("id") Long userId, @PathVariable("size") Integer size);
    @PostMapping("/chirper/basic_info")
    ResponseEntity<List<ChirperDto>> getBasicInfo(@RequestParam("ids") List<Long> ids);

    @PostMapping("/chirper/id/author")
    ResponseEntity<Map<Long, List<Long>>> getIdByAuthor(@RequestParam("userIds") Collection<Long> userIds);

    //likeController

}
