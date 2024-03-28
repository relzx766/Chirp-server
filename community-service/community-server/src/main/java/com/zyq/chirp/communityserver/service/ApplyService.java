package com.zyq.chirp.communityserver.service;

import com.zyq.chirp.communityclient.dto.ApplyDto;
import com.zyq.chirp.communityclient.dto.CommunityQueryDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ApplyService {
    void addOne(ApplyDto applyDto);

    void approve(ApplyDto applyDto);

    List<ApplyDto> getPage(CommunityQueryDto queryDto);

    Map<Long, ApplyDto> getByUserId(Collection<Long> communityIds, Long userId);
}
