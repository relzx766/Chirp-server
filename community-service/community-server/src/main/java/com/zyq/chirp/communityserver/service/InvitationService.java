package com.zyq.chirp.communityserver.service;

import com.zyq.chirp.communityclient.dto.InvitationDto;

import java.util.Collection;
import java.util.List;

public interface InvitationService {
    void send(InvitationDto invitationDto);

    void addBatch(Collection<InvitationDto> invitationDtos);

    void accept(InvitationDto invitationDto);

    void reject(InvitationDto invitationDto);

    List<InvitationDto> getByToId(Long toId);

    List<InvitationDto> getById(Collection<Long> ids);
}