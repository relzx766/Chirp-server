package com.zyq.chirp.userserver.service;


import com.zyq.chirp.userclient.dto.RelationDto;
import com.zyq.chirp.userserver.model.pojo.Relation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RelationService {
    Relation getRelationType(Long fromId, Long toId);

    /**
     * 获取我与用户名的关系，以我为起点，即我=fromId，用户们=toId
     *
     * @param userIds      用户们
     * @param targetUserId 我
     * @return 关系列表，未查出的以默认关系替代
     */
    List<RelationDto> getUserRelation(Collection<Long> userIds, Long targetUserId);

    /**
     * 获取用户间的关系
     *
     * @param userList 用户列表，格式为fromId:toId
     * @return 关系集合
     */
    Map<String, RelationDto> getUserRelation(Collection<String> userList);

    /**
     * 获取我与用户名的关系，以用户们为起点，即用户们=fromId，我=toId
     * @param userIds 用户们
     * @param targetUserId 我
     * @return 关系列表，未查出的以默认关系替代
     */

    List<RelationDto> getUserRelationReverse(Collection<Long> userIds, Long targetUserId);




    List<Long> getFollower(Long userId, Integer page, Integer pageSize);

    List<Long> getFollowing(Long userId);

    List<Long> getFollowing(Long userId, Integer page, Integer pageSize);

    Long getFollowerCount(Long userId);

    Long getFollowingCount(Long userId);

    void follow(Long fromId, Long toId);

    void unfollow(Long fromId, Long toId);

    void block(Long fromId, Long toId);

    void cancelBlock(Long fromId, Long toId);
}
