package com.zyq.chirp.communityserver.convertor;

import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.communityclient.dto.MemberDto;
import com.zyq.chirp.communityserver.domain.pojo.Community;
import com.zyq.chirp.communityserver.domain.pojo.Member;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")

public interface MemberConvertor {
    Member dtoToPojo(MemberDto memberDto);

    MemberDto pojoToDto(Member member);
}
