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


    @PostMapping("/chirper/short")
    ResponseEntity<List<ChirperDto>> getShort(@RequestParam("ids") List<Long> ids);

    //likeController

}
