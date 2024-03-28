package com.zyq.chirp.communityserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.common.domain.enums.ApproveEnum;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.communityclient.dto.InvitationDto;
import com.zyq.chirp.communityclient.dto.MemberDto;
import com.zyq.chirp.communityserver.convertor.InvitationConvertor;
import com.zyq.chirp.communityserver.domain.enums.CommunityJoinRangeEnum;
import com.zyq.chirp.communityserver.domain.pojo.Invitation;
import com.zyq.chirp.communityserver.mapper.InvitationMapper;
import com.zyq.chirp.communityserver.service.CommunityService;
import com.zyq.chirp.communityserver.service.InvitationService;
import com.zyq.chirp.communityserver.service.MemberService;
import com.zyq.chirp.communityserver.util.CommunityUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InvitationServiceImpl extends ServiceImpl<InvitationMapper, Invitation> implements InvitationService {
    @Resource
    CommunityService communityService;
    @Resource
    MemberService memberService;
    @Resource
    InvitationConvertor convertor;
    @Resource
    InvitationMapper invitationMapper;
    @Resource
    KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${mq.topic.community.invite}")
    String TOPIC_INVITE;

    @Override
    public void send(InvitationDto invitationDto) {
        invitationDto.setFromId(StpUtil.getLoginIdAsLong());
        if (invitationDto.getFromId().equals(invitationDto.getToId())) {
            throw new ChirpException(Code.ERR_BUSINESS, "🙄🙄🙄");
        }
        CommunityDto communityDto = communityService.getById(invitationDto.getId());
        MemberDto memberDto = memberService.getOne(invitationDto.getFromId(), invitationDto.getCommunity().getId());
        if (CommunityUtil.canJoin(communityDto.getJoinRange(), memberDto.getRole())) {
            Invitation invitation = convertor.dtoToPojo(invitationDto);
            invitation.setCreateTime(Timestamp.valueOf(LocalDateTime.now()));
            invitation.setStatus(ApproveEnum.PENDING.getStatus());
            save(invitation);
            NotificationDto notificationDto = NotificationDto.builder()
                    .senderId(invitationDto.getFromId())
                    .receiverId(invitationDto.getFromId())
                    .sonEntity(invitation.getId().toString())
                    .build();
            kafkaTemplate.send(TOPIC_INVITE, notificationDto);
        } else {
            throw new ChirpException(Code.ERR_BUSINESS, CommunityJoinRangeEnum.find(communityDto.getJoinRange()).getMessage());
        }

    }

    @Override
    public void addBatch(Collection<InvitationDto> invitationDtos) {
        if (!CollectionUtils.isEmpty(invitationDtos)) {
            List<Invitation> invitations = invitationDtos.stream().map(invitationDto -> convertor.dtoToPojo(invitationDto)).toList();
            saveBatch(invitations);
        }
    }

    @Override
    public void accept(InvitationDto invitationDto) {
        communityService.join(invitationDto);
        invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                .set(Invitation::getStatus, ApproveEnum.ACCEPTED.getStatus())
                .set(Invitation::getUpdateTime, Timestamp.valueOf(LocalDateTime.now()))
                .eq(Invitation::getId, invitationDto.getId()));
    }

    @Override
    public void reject(InvitationDto invitationDto) {
        invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                .set(Invitation::getStatus, ApproveEnum.REJECTED.getStatus())
                .set(Invitation::getUpdateTime, Timestamp.valueOf(LocalDateTime.now()))
                .eq(Invitation::getId, invitationDto.getId()));
    }

    @Override
    public List<InvitationDto> getByToId(Long toId) {
        return invitationMapper.selectList(new LambdaQueryWrapper<Invitation>().eq(Invitation::getToId, toId))
                .stream()
                .map(invitation -> convertor.pojoToDto(invitation)).toList();
    }

    @Override
    public List<InvitationDto> getById(Collection<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            List<InvitationDto> invitationDtos = new ArrayList<>();
            List<Long> communityIds = new ArrayList<>();
            invitationMapper.selectList(new LambdaQueryWrapper<Invitation>()
                            .in(Invitation::getId, ids))
                    .forEach(invitation -> {
                        InvitationDto invitationDto = convertor.pojoToDto(invitation);
                        invitationDtos.add(invitationDto);
                        communityIds.add(invitationDto.getCommunity().getId());
                    });
            Map<Long, CommunityDto> communityDtoMap = communityService.getById(communityIds)
                    .stream()
                    .collect(Collectors.toMap(CommunityDto::getId, Function.identity()));
            return invitationDtos.stream()
                    .peek(invitationDto -> invitationDto.setCommunity(communityDtoMap.get(invitationDto.getCommunity().getId()))).toList();
        }
        return List.of();
    }
}
