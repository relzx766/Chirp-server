package com.zyq.chirp.userserver.service;


import com.zyq.chirp.userserver.model.pojo.Relation;

import java.util.Collection;
import java.util.List;

public interface RelationService {
    Relation getRelationType(Long fromId, Long toId);

    List<Relation> getUserRelation(Collection<Long> userIds, Long targetUserId);

    List<Long> getFollower(Long userId, Integer page, Integer pageSize);

    Long getFollowerCount(Long userId);

    Long getFollowingCount(Long userId);

    void follow(Long fromId, Long toId);

    void unfollow(Long fromId, Long toId);

    void block(Long fromId, Long toId);

    void cancelBlock(Long fromId, Long toId);
}
