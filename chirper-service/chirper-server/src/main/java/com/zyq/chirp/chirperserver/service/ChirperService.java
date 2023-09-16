package com.zyq.chirp.chirperserver.service;

import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.domain.enums.ChirperStatus;
import com.zyq.chirp.chirperserver.domain.enums.ChirperType;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ChirperService {
    ChirperDto save(ChirperDto chirperDto);

    ChirperDto reply(ChirperDto chirperDto);

    /**
     * 获取被引用的推文
     *
     * @param ids 被引用者
     * @return k:被引用者id v:被引用者
     */

    Map<Long, ChirperDto> fetchReference(Collection<Long> ids);

    /**
     * 获取互动信息，如点赞，引用
     *
     * @param chirperDtos
     * @param userId
     * @return
     */
    List<ChirperDto> getInteractionInfo(List<ChirperDto> chirperDtos, Long userId);

    void forward(Long chirperId, Long userId);

    /**
     * @param chirperId 被转发的推文id
     * @param userId    转发者
     */
    void cancelForward(Long chirperId, Long userId);


    ChirperDto quote(ChirperDto chirperDto);


    void delete(Long chirperId, Long currentUserId);


    ChirperDto getById(Long chirperId);

    List<ChirperDto> getById(Collection<Long> chirperIds);

    List<ChirperDto> getPage(Integer page);

    List<ChirperDto> search(String keyword, Integer page, Boolean isMedia);

    List<ChirperDto> getChildChirper(Long chirperId, Integer page);

    List<ChirperDto> getByUserId(Long userId, Integer page, @Nullable ChirperType type, @Nullable Boolean isMedia);

    List<ChirperDto> getLikeRecordByUserId(Long userId, Integer page);

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


    /**
     * 获取基础的推文信息，不包含互动等消息
     *
     * @param chirperIds
     * @return
     */

    List<ChirperDto> getShort(Collection<Long> chirperIds);


    List<ChirperDto> combineWithMedia(Collection<ChirperDto> chirperDtos);

}
