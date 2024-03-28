package com.zyq.chirp.communityclient.client;

import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.communityclient.dto.InvitationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@FeignClient("community-server")
public interface CommunityClient {
    @GetMapping("/invite/fetch")
    ResponseEntity<List<InvitationDto>> getById(@RequestBody Collection<Long> ids);

    @PostMapping("/community/fetch/map")
    ResponseEntity<Map<String, CommunityDto>> fetchMap(@RequestBody List<Map.Entry<Long, Long>> mapList);

}
