package com.zyq.chirp.chirperserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirpclient.dto.ChirperQueryDto;
import com.zyq.chirp.chirperserver.domain.enums.ChirperType;
import com.zyq.chirp.chirperserver.service.ChirperService;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chirper")
public class ChirperController {
    @Resource
    ChirperService chirperService;
    @Resource
    ObjectMapper objectMapper;

    @PostMapping("/add")
    public ResponseEntity<ChirperDto> addChirper(

            @RequestBody @Validated ChirperDto chirperDto) {
        chirperDto.setAuthorId(StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok().body(chirperService.save(chirperDto));
    }

    @PostMapping("/reply")
    public ResponseEntity<ChirperDto> reply(@RequestBody @Validated(ChirperDto.Reply.class) ChirperDto chirperDto) {
        chirperDto.setAuthorId(StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(chirperService.reply(chirperDto));
    }

    @PostMapping("/forward")
    public ResponseEntity<Boolean> forwardChirper(
            @RequestParam("chirperId") Long chirperId) {
        chirperService.forward(chirperId, StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(true);
    }

    @PostMapping("/forward/cancel")
    public ResponseEntity<Boolean> cancelForward(@RequestParam("chirperId") Long chirperId) {
        chirperService.cancelForward(chirperId, StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(true);
    }

    @PostMapping("/quote")
    public ResponseEntity<ChirperDto> quoteChirper(

            @RequestBody @Validated(ChirperDto.Quote.class) ChirperDto chirperDto) {
        chirperDto.setAuthorId(StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(chirperService.quote(chirperDto));
    }


    //@ApiOperation(value = "推文详情")
    @GetMapping("/detail/{id}")
    public ResponseEntity<ChirperDto> getDetail(@PathVariable("id") Long id) {
        List<ChirperDto> chirperDtos = chirperService.getById(List.of(id));
        if (StpUtil.isLogin()) {
            chirperDtos = chirperService.getInteractionInfo(chirperDtos, StpUtil.getLoginIdAsLong());
        }
        return ResponseEntity.ok(chirperDtos.get(0));
    }

    @PostMapping("/page/id")
    public ResponseEntity<List<ChirperDto>> getByIds(@RequestParam("ids") List<Long> ids) {
        List<ChirperDto> chirperDtos = chirperService.getById(ids);
        if (StpUtil.isLogin() && chirperDtos != null && !chirperDtos.isEmpty()) {
            chirperDtos = chirperService.getInteractionInfo(chirperDtos, StpUtil.getLoginIdAsLong());
        }
        return ResponseEntity.ok(chirperDtos);
    }

    @PostMapping("/page")
    public ResponseEntity<List<ChirperDto>> getPage(@RequestBody ChirperQueryDto chirperQueryDto) {
        if (StpUtil.isLogin()) {
            chirperQueryDto.setCurrentUserId(StpUtil.getLoginIdAsLong());
        }
        List<ChirperDto> chirperDtos = chirperService.getPage(chirperQueryDto);
        if (chirperQueryDto.getCurrentUserId() != null) {
            chirperService.getInteractionInfo(chirperDtos, chirperQueryDto.getCurrentUserId());
        }
        return ResponseEntity.ok(chirperDtos);
    }
    @GetMapping("/like/{id}/{page}")
    public ResponseEntity<List<ChirperDto>> getLikeRecord(@PathVariable("id") Long id,
                                                          @PathVariable("page") Integer page) {
        List<ChirperDto> chirperDtos = chirperService.getLikeRecordByUserId(id, page);
        if (StpUtil.isLogin()) {
            chirperService.getInteractionInfo(chirperDtos, StpUtil.getLoginIdAsLong());
        }
        return ResponseEntity.ok(chirperDtos);
    }

    @PostMapping("/basic_info")
    public ResponseEntity<List<ChirperDto>> getShort(@RequestParam("ids") List<Long> ids) {
        return ResponseEntity.ok(chirperService.getBasicInfo(ids));
    }

    @PostMapping("/content")
    public ResponseEntity<List<ChirperDto>> getContent(@RequestParam("ids") List<Long> ids, @RequestParam("userId") Long userId) {
        List<ChirperDto> chirperDtoList = chirperService.getById(ids);
        if (userId != null) {
            chirperService.getInteractionInfo(chirperDtoList, userId);
        }
        return ResponseEntity.ok(chirperDtoList);
    }

    @GetMapping("/trend/{page}")
    public ResponseEntity<Map<Object, Map<String, Object>>> getTrend(@PathVariable("page") Integer page,
                                                                     @RequestParam("type") String type) {
        return ResponseEntity.ok(chirperService.getTrend(page, type));
    }

    @PostMapping("/id/author")
    public ResponseEntity<Map<Long, List<Long>>> getIdByAuthor(@RequestParam("userIds") Collection<Long> userIds) {
        return ResponseEntity.ok(chirperService.getAllIdByAuthors(userIds));
    }

    @GetMapping("/following/{id}/{size}")
    public ResponseEntity<List<ChirperDto>> getByFollowerId(@PathVariable("id") Long userId, @PathVariable("size") Integer size) {
        return ResponseEntity.ok(chirperService.getByFollowerId(userId, size));
    }
}
