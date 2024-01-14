package com.zyq.chirp.chirperserver.service;

import com.zyq.chirp.chirpclient.dto.LikeDto;
import com.zyq.chirp.chirperserver.domain.pojo.Like;
import com.zyq.chirp.common.mq.model.Action;

import java.util.Collection;
import java.util.List;

public interface LikeService {


    void addLike(LikeDto likeDto);

    void saveLike(List<Action<Long, Long>> actions);

    void cancelLike(LikeDto likeDto);

    void saveLikeCancel(List<Action<Long, Long>> actions);

    void modifyLikeCount(List<Action<Long, Long>> actions);

    /**
     * 获取用户对推文的点赞信息
     *
     * @param chirperIds
     * @param userId
     * @return 有点赞记录的推文id
     */
    List<Long> getLikeInfo(Collection<Long> chirperIds, Long userId);

    List<Like> getLikeRecord(Long userId, Integer page);

    String getKey(LikeDto likeDto);

    boolean addList(List<Like> likes);

    boolean deleteList(List<Like> likes);

    int deleteByChirperId(List<Long> chirperIds);

    boolean updateLikeCount(Long chirperId, Integer delta);
}
