package com.zyq.chirp.chirperserver.service;

import com.zyq.chirp.chirpclient.dto.LikeDto;
import com.zyq.chirp.chirperserver.domain.pojo.Like;

import java.util.List;

public interface LikeService {


    void addLike(LikeDto likeDto);

    void cancelLike(LikeDto likeDto);


    String getKey(LikeDto likeDto);

    boolean addList(List<Like> likes);

    boolean deleteList(List<Like> likes);

    int deleteByChirperId(List<Long> chirperIds);

    boolean updateLikeCount(Long chirperId, Integer delta);
}
