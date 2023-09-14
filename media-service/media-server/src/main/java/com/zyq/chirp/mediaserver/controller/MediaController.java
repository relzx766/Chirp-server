package com.zyq.chirp.mediaserver.controller;

import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import com.zyq.chirp.mediaserver.service.MediaService;
import com.zyq.chirp.mediaserver.util.FileUtil;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/media")
public class MediaController {
    @Resource
    MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<MediaDto> upload(@RequestParam("file") MultipartFile file, @RequestParam("hash") String hash) {
        MediaDto mediaDto = new MediaDto();
        mediaDto.setMd5(hash);
        mediaDto.setExtension(FileUtil.getExtension(file.getOriginalFilename()));
        try {
            return ResponseEntity.ok(mediaService.saveFile(file.getBytes(), mediaDto));
        } catch (IOException e) {
            throw new ChirpException(Code.ERR_BUSINESS, e);
        }
    }

    @PostMapping("/fast")
    public ResponseEntity<MediaDto> fastUpload(@RequestParam("hash") String hash) {
        MediaDto mediaDto = mediaService.getByMd5(hash);
        return mediaDto != null ? ResponseEntity.ok(mediaDto) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/upload/slice")
    public ResponseEntity<MediaDto> uploadSlice(@RequestParam("file") MultipartFile file,
                                                @RequestParam("hash") String hash,
                                                @RequestParam("seq") Integer seq) {
        try {
            mediaService.saveSlice(file.getBytes(), hash, file.getOriginalFilename(), seq);
            return ResponseEntity.ok(null);
        } catch (IOException e) {
            throw new ChirpException(Code.ERR_SYSTEM, "上传失败");
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<MediaDto> merge(@RequestBody MediaDto mediaDto) {
        MediaDto file = mediaService.mergeFile(mediaDto);
        return ResponseEntity.ok(file);
    }

    @PostMapping("/url/get")
    public ResponseEntity<List<MediaDto>> getUrl(@RequestParam("id") List<Integer> id) {
        return ResponseEntity.ok(mediaService.getUrlById(id));
    }

    @PostMapping("/combine/get")
    public ResponseEntity<Map<Long, List<MediaDto>>> getCombine(@RequestBody Map<Long, List<Integer>> map) {
        return ResponseEntity.ok(mediaService.getByMap(map));
    }
}
