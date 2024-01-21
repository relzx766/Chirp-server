package com.zyq.chirp.chirperserver.service;

import com.zyq.chirp.chirpclient.dto.ChirperDto;
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
    /*    void saveForward(List<Action<Long,Long>>actions);*/

    /**
     * @param chirperId 被转发的推文id
     * @param userId    转发者
     */
    boolean cancelForward(Long chirperId, Long userId);

    /*   void saveForwardCancel(List<Action<Long,Long>>actions);*/
    void modifyForwardCount(List<Action<Long, Long>> actions);

    ChirperDto quote(ChirperDto chirperDto);

    void modifyQuoteCount(List<Action<Long, Long>> actions);


    void delete(Long chirperId, Long currentUserId);


    List<ChirperDto> getById(List<Long> chirperIds);

    List<ChirperDto> getPage(Integer page, Long chirperId, Collection<Long> userIds, ChirperType type, Boolean isMedia, String order);

    List<ChirperDto> search(String keyword, Integer page, Boolean isMedia);

    List<ChirperDto> getFollowing(Integer page, Long userId);


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
     * 获取基础的推文信息
     *
     * @param chirperIds
     * @return
     */

    List<ChirperDto> getBasicInfo(Collection<Long> chirperIds);


    List<ChirperDto> combineWithMedia(Collection<ChirperDto> chirperDtos);

    Map<Object, Map<String, Object>> getTrend(Integer page, String type);

    Map<Long, List<Long>> getAllIdByAuthors(Collection<Long> userIds);

    Long getAuthorIdByChirperId(Long chirperId);

    List<ChirperDto> getByFollowerId(Long userId, Integer size);

    Map<Long, Boolean> getReplyable(List<ChirperDto> chirperDtos, Long userId);

    ChirperDto getWithPrecondition(ChirperDto chirperDto);

    void postDelay(ChirperDto chirperDto);

    void activeDelay(Collection<Long> chirperIds);

    void activeDelayAuto();
}
