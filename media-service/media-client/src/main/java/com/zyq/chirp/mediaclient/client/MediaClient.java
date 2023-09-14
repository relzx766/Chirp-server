package com.zyq.chirp.mediaclient.client;

import com.zyq.chirp.mediaclient.dto.MediaDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient("media-server")
public interface MediaClient {
    @PostMapping("/media/url/get")
    ResponseEntity<List<MediaDto>> getUrl(@RequestParam("id") List<Integer> id);

    @PostMapping("/media/combine/get")
    ResponseEntity<Map<Long, List<MediaDto>>> getCombine(@RequestBody Map<Long, List<Integer>> map);
}
