package com.zyq.chirp.chirperserver.service;

import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirpclient.dto.ChirperQueryDto;
import com.zyq.chirp.chirperserver.domain.enums.ChirperStatus;
import com.zyq.chirp.chirperserver.domain.enums.ChirperType;
import com.zyq.chirp.common.mq.model.Action;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ChirperService {
    ChirperDto save(ChirperDto chirperDto);

    ChirperDto reply(ChirperDto chirperDto);

    void modifyReplyCount(List<Action<Long, Long>> actions);

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
    boolean cancelForward(Long chirperId, Long userId);
    void modifyForwardCount(List<Action<Long, Long>> actions);
    ChirperDto quote(ChirperDto chirperDto);
    void modifyQuoteCount(List<Action<Long, Long>> actions);
    List<ChirperDto> getById(List<Long> chirperIds);

    List<ChirperDto> getPage(ChirperQueryDto chirperQueryDto);


    List<ChirperDto> getLikeRecordByUserId(Long userId, Integer page);

    void updateStatus(Long chirperId, ChirperStatus chirperStatus);


    /**
     * 获取基础的推文信息
     *
     * @param chirperIds
     * @return
     */

    List<ChirperDto> getBasicInfo(Collection<Long> chirperIds);


    List<ChirperDto> combine(Collection<ChirperDto> chirperDtos);

    Map<Object, Map<String, Object>> getTrend(Integer page, String type);

    Map<Long, List<Long>> getAllIdByAuthors(Collection<Long> userIds);


    List<ChirperDto> getByFollowerId(Long userId, Integer size);

    /**
     * 获取推文的互动可用性，即能否评论，能否转发，能否引用，能否点赞
     *
     * @param chirperDtos
     * @param userId
     * @return
     */
    List<ChirperDto> getInteractionStatus(List<ChirperDto> chirperDtos, Long userId);

    ChirperDto getWithPrecondition(ChirperDto chirperDto);

    void postDelay(ChirperDto chirperDto);

    void activeDelay(Collection<Long> chirperIds);

    void activeDelayAuto();
}
