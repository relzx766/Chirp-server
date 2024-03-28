package com.zyq.chirp.communityserver.service;

import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.communityclient.dto.MemberDto;
import com.zyq.chirp.communityclient.dto.CommunityQueryDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MemberService {
    void add(Long userId, Long communityId);

    void add(MemberDto memberDto);

    void leave(MemberDto memberDto);

    void update(MemberDto memberDto);

    MemberDto getOne(Long userId, Long communityId);

    Map<Long, MemberDto> getMap(Long communityId, Collection<Long> userIds);

    List<MemberDto> getByUserId(Long userId, List<Long> communityId);

    List<MemberDto> getModeratorOfCommunity(Long communityId);

    List<MemberDto> getMemberOfCommunity(Long communityId, Integer page);

    Long getCountOfCommunity(Long communityId);

    Map<Long, Long> getCountOfCommunity(List<Long> communityIds);

    List<MemberDto> getPage(CommunityQueryDto communityQueryDto);

    Map<Long, List<MemberDto>> getMemberOfCommunity(List<Long> communityIds, Integer size);


    /**
     * @param maps key:communityId value:userId
     * @return key:communityId:userId
     */
    Map<String, MemberDto> getMap(List<Map.Entry<Long, Long>> maps);
}
