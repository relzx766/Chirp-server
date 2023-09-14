package com.zyq.chirp.chirperserver.service;

import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.domain.enums.ChirperStatus;

import java.util.Collection;
import java.util.List;

public interface ChirperService {
    ChirperDto save(ChirperDto chirperDto);

    ChirperDto reply(ChirperDto chirperDto);

    ChirperDto delayPost(ChirperDto chirperDto, Long delay);

    void forward(Long chirperId, Long userId);

    /**
     * @param chirperId 被转发的推文id
     * @param userId    转发者
     */
    void cancelForward(Long chirperId, Long userId);

    /**
     * 根据被引用推文id与作者id，获取目前推文
     * referencedChirperId-authorId
     * \/
     * id
     *
     * @param chirperDtos
     * @return 推文列表
     */
    List<ChirperDto> getByReference(List<ChirperDto> chirperDtos);

    ChirperDto quote(ChirperDto chirperDto);


    void delete(Long chirperId, Long currentUserId);


    ChirperDto getById(Long chirperId, Long currentUserId);

    List<ChirperDto> getPage(Integer page, Long userId);

    List<ChirperDto> search(String keyword, Integer page, Long currentUserId, Boolean isMedia);

    List<ChirperDto> getChildChirper(Long chirperId, Integer page, Long currentUserId);

    List<ChirperDto> getByAuthorId(Long authorId, Integer page, Long currentUserId);

    void updateStatus(Long chirperId, ChirperStatus chirperStatus);

    /**
     * 更新互动值
     *
     * @param chirperId
     * @param delta
     * @return
     */
    int updateView(Long chirperId, Integer delta);

    /**
     * 更新转发值
     *
     * @param chirperId
     * @param delta
     * @return
     */
    int updateForward(Long chirperId, Integer delta);

    Long getAuthorIdByChirperId(Long chirperId);

    ChirperDto getShort(Long chirperId);

    /**
     * 获取基础的推文信息，不包含互动等消息
     *
     * @param chirperIds
     * @return
     */

    List<ChirperDto> getShort(Collection<Long> chirperIds);

    List<ChirperDto> combineWithMedia(List<ChirperDto> chirperDtos);

}
