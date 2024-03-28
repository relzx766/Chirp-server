package com.zyq.chirp.communityserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.common.domain.model.QueryDto;
import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.communityserver.service.CommunityService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/community")
public class CommunityController {
    @Resource
    CommunityService communityService;

    @PostMapping("/save")
    public ResponseEntity<CommunityDto> add(@RequestBody CommunityDto communityDto) {
        return ResponseEntity.ok(communityService.save(communityDto));
    }

    @PostMapping("/update")
    public ResponseEntity<CommunityDto> update(@RequestBody CommunityDto communityDto) {
        return ResponseEntity.ok(communityService.update(communityDto));
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<CommunityDto> getDetail(@PathVariable("id") Long id) {
        CommunityDto communityDto = communityService.getById(id);
        if (StpUtil.isLogin()) {
            communityDto = communityService.assemble(List.of(communityDto), StpUtil.getLoginIdAsLong()).getFirst();
        }
        return ResponseEntity.ok(communityDto);
    }

    @PostMapping("/join")
    public ResponseEntity<CommunityDto> join(@RequestBody CommunityDto communityDto) {
        return ResponseEntity.ok(communityService.join(communityDto.getId(), StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/page")
    public ResponseEntity<List<CommunityDto>> getPage(@RequestBody QueryDto queryDto) {
        List<CommunityDto> communityDtos = communityService.getPage(queryDto);
        if (StpUtil.isLogin()) {
            return ResponseEntity.ok(communityService.assemble(communityDtos, StpUtil.getLoginIdAsLong()));
        }
        return ResponseEntity.ok(communityDtos);
    }

    @PostMapping("/leave")
    public ResponseEntity<Boolean> leave(@RequestParam("communityId") Long communityId,
                                         @RequestParam("userId") Long userId) {
        communityService.leave(communityId, userId);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/role/update")
    public ResponseEntity<Boolean> updateRole(@RequestParam("communityId") Long communityId,
                                              @RequestParam("userId") Long userId,
                                              @RequestParam("role") Integer role) {
        communityService.updateRole(communityId, userId, role);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/fetch/{communityId}/{userId}")
    public ResponseEntity<CommunityDto> fetchOne(@PathVariable("communityId") Long communityId,
                                                 @PathVariable("userId") Long userId) {
        CommunityDto communityDto = communityService.getById(communityId);
        communityDto = communityService.assemble(List.of(communityDto), userId).getFirst();
        return ResponseEntity.ok(communityDto);
    }

    @PostMapping("/fetch/map")
    public ResponseEntity<Map<String, CommunityDto>> fetchMap(@RequestBody List<Map.Entry<Long, Long>> mapList) {
        return ResponseEntity.ok(communityService.getMap(mapList));
    }
}
