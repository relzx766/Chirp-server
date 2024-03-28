package com.zyq.chirp.mediaserver.service;

import com.zyq.chirp.mediaclient.dto.ChunkUploadReqDto;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface MediaService {
    MediaDto getById(Integer id);

    List<MediaDto> getById(List<Integer> id);

    /***
     * 批量获取推文的媒体文件
     * @param map k:推文id v:媒体id列表
     * @return k:推文id v:媒体列表
     */
    Map<Long, List<MediaDto>> getByMap(Map<Long, List<Integer>> map);


    List<MediaDto> getUrlById(List<Integer> id);

    /***
     * 批量获取推文的媒体文件
     * @param map k:推文id v:媒体id列表
     * @return k:推文id v:媒体列表
     */
    Map<Long, List<MediaDto>> getUrlByMap(Map<Long, List<Integer>> map);

    MediaDto getByMd5(String md5);

    List<MediaDto> getByIdList(List<Integer> ids);

    MediaDto save(MediaDto mediaDto);

    MediaDto saveFile(MultipartFile file);

    ChunkUploadReqDto initChunkUpload(int chunkSize);

    void uploadChunk(String uploadId, int index, MultipartFile file);

    String chunkHashCache(String uploadId, int index, MultipartFile file);

    MediaDto mergeFile(ChunkUploadReqDto reqDto);

    MediaDto saveFile(byte[] file, MediaDto mediaDto);

    void saveSlice(byte[] file, String hash, String filename, Integer seq);

    /*  MediaDto mergeFile(MediaDto mediaDto);*/

}
