package com.zyq.chirp.mediaserver.service;

import com.zyq.chirp.mediaclient.dto.MediaDto;

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

    MediaDto getUrlById(Integer id);

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

    MediaDto saveFile(byte[] file, MediaDto mediaDto);

    void saveSlice(byte[] file, String hash, String filename, Integer seq);

    MediaDto mergeFile(MediaDto mediaDto);

}
