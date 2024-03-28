package com.zyq.chirp.communityserver.service;


import com.zyq.chirp.common.domain.model.QueryDto;
import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.communityclient.dto.InvitationDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CommunityService {
    CommunityDto save(CommunityDto communityDto);

    CommunityDto update(CommunityDto communityDto);

    CommunityDto getById(Long id);

    List<CommunityDto> getById(Collection<Long> ids);

    CommunityDto join(Long communityId, Long userId);

    void join(InvitationDto invitationDto);

    List<CommunityDto> getPage(QueryDto queryDto);

    List<CommunityDto> assemble(List<CommunityDto> communityDtos, Long userId);

    void leave(Long communityId, Long userId);

    void updateRole(Long communityId, Long userId, Integer role);


    /**
     * @param maps key:communityId value:userId
     * @return key:communityId:userId value:CommunityDto
     */
    Map<String, CommunityDto> getMap(List<Map.Entry<Long, Long>> maps);
}
