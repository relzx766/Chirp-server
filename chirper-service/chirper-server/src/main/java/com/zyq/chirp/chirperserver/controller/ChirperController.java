package com.zyq.chirp.chirperserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.domain.enums.ChirperType;
import com.zyq.chirp.chirperserver.service.ChirperService;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chirper")
public class ChirperController {
    @Resource
    ChirperService chirperService;
    @Resource
    ObjectMapper objectMapper;

    //    @ApiOperation(value = "发布推文", notes = "发布原创推文")
    @PostMapping("/add")
    public ResponseEntity<ChirperDto> addChirper(
          /*  @ApiParam(value = "推文", required = true, example = """
                    {
                        "text":"推文的内容",
                        "mediaKeys":"媒体链接，json格式"
                        text与mediaKeys有一个不为空
                    }""")*/
            @RequestBody ChirperDto chirperDto) {
        chirperDto.setAuthorId(StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok().body(chirperService.save(chirperDto));
    }

    @PostMapping("/reply")
    public ResponseEntity<ChirperDto> reply(@RequestBody ChirperDto chirperDto) {
        chirperDto.setAuthorId(StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(chirperService.reply(chirperDto));
    }

    //@ApiOperation(value = "转发推文", notes = "不修改源推文，仅做转发")
    @PostMapping("/forward")
    public ResponseEntity forwardChirper(
            // @ApiParam(value = "推文id", required = true, example = "123")
            @RequestParam("chirperId") Long chirperId) {
        chirperService.forward(chirperId, StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(null);
    }

    @PostMapping("/forward/cancel")
    public ResponseEntity cancelForward(@RequestParam("chirperId") Long chirperId) {
        chirperService.cancelForward(chirperId, StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(null);
    }

    // @ApiOperation(value = "引用推文", notes = "需要加上自己的内容")
    @PostMapping("/quote")
    public ResponseEntity<ChirperDto> quoteChirper(
/*            @ApiParam(value = "推文", required = true, example = """
                    {

                        "text":"推文的内容",
                        "mediaKeys":"媒体链接，json格式"
                        "referencedChirperId":"引用的推文id"
                        text与mediaKeys有一个不为空
                    }""")*/
            @RequestBody ChirperDto chirperDto) {
        chirperDto.setAuthorId(StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(chirperService.quote(chirperDto));
    }

    //@ApiOperation(value = "删除推文",notes = "用户仅能删除其自己的推文")
    @DeleteMapping("/delete")
    public ResponseEntity del(
            //   @ApiParam(value = "推文id",required = true,example = "123")
            @RequestParam("chirperId") Long chirperId) {
        chirperService.delete(chirperId, StpUtil.getLoginIdAsLong());
        return ResponseEntity.ok(null);
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

    @GetMapping("/page/{page}")
    public ResponseEntity<List<ChirperDto>> getPage(@PathVariable("page") Integer page) {
        List<ChirperDto> chirperDtos = chirperService.getPage(page);
        if (StpUtil.isLogin()) {
            chirperService.getInteractionInfo(chirperDtos, StpUtil.getLoginIdAsLong());
        }
        return ResponseEntity.ok(chirperDtos);
    }

    @PostMapping("/search")
    public ResponseEntity<List<ChirperDto>> search(@RequestParam("keyword") String keyword,
                                                   @RequestParam("page") Integer page,
                                                   @RequestParam("isMedia") Boolean isMedia) {

        List<ChirperDto> chirperDtos = chirperService.search(keyword, page, isMedia);
        if (StpUtil.isLogin()) {
            chirperService.getInteractionInfo(chirperDtos, StpUtil.getLoginIdAsLong());
        }
        return ResponseEntity.ok(chirperDtos);
    }

    @GetMapping("/child/{id}/{page}")
    public ResponseEntity<List<ChirperDto>> getChildChirper(@PathVariable("id") Long chirperId,
                                                            @PathVariable("page") Integer page) {

        List<ChirperDto> chirperDtos = chirperService.getChildChirper(chirperId, page);
        if (StpUtil.isLogin()) {
            chirperService.getInteractionInfo(chirperDtos, StpUtil.getLoginIdAsLong());
        }
        return ResponseEntity.ok(chirperDtos);
    }

    @GetMapping("/author/{id}/{page}")
    public ResponseEntity<List<ChirperDto>> getByAuthor(@PathVariable("id") Long authorId,
                                                        @PathVariable("page") Integer page,
                                                        @Nullable @RequestParam("type") String type,
                                                        @Nullable @RequestParam("media") Boolean media) {


        List<ChirperDto> chirperDtos = chirperService.getByUserId(authorId, page, ChirperType.find(type), media);
        if (StpUtil.isLogin()) {
            chirperService.getInteractionInfo(chirperDtos, StpUtil.getLoginIdAsLong());
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

    @PostMapping("/short")
    public ResponseEntity<List<ChirperDto>> getShort(@RequestParam("ids") List<Long> ids) {
        return ResponseEntity.ok(chirperService.getShort(ids));
    }


}
