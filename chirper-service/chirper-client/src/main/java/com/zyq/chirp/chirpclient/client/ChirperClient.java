package com.zyq.chirp.chirpclient.client;

import com.zyq.chirp.chirpclient.dto.ChirperDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("chirper-server")
public interface ChirperClient {
    //chirperController

    @PostMapping("/chirper/content")
    ResponseEntity<List<ChirperDto>> getContent(@RequestParam("ids") List<Long> ids);

    @PostMapping("/chirper/basic_info")
    ResponseEntity<List<ChirperDto>> getBasicInfo(@RequestParam("ids") List<Long> ids);

    //likeController

}
