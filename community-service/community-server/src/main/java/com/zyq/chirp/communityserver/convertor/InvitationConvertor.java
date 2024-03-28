package com.zyq.chirp.communityserver.convertor;

import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.communityclient.dto.InvitationDto;
import com.zyq.chirp.communityserver.domain.pojo.Invitation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface InvitationConvertor {
    @Mapping(target = "communityId", source = "community.id")
    Invitation dtoToPojo(InvitationDto invitationDto);

    @Mapping(target = "community", source = "communityId", qualifiedByName = "pojoCommunityToDto")
    InvitationDto pojoToDto(Invitation invitation);

    @Named("pojoCommunityToDto")
    default CommunityDto getCommunity(Long id) {
        return CommunityDto.builder().id(id).build();
    }
}
