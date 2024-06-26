package com.zyq.chirp.adviceserver.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.domain.enums.NoticeEntityTypeEnums;
import com.zyq.chirp.adviceserver.strategy.MessageAssembleStrategy;
import com.zyq.chirp.chirpclient.client.ChirperClient;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.communityclient.client.CommunityClient;
import com.zyq.chirp.communityclient.dto.InvitationDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NoticeAssembleStrategy implements MessageAssembleStrategy<NotificationDto> {
    @Resource
    DefaultMessageAssembleStrategy<NotificationDto> defaultMessageAssembleStrategy;
    @Resource
    ChirperClient chirperClient;
    @Resource
    CommunityClient communityClient;
    @Resource
    ObjectMapper objectMapper;

    @Override
    public List<NotificationDto> assemble(List<NotificationDto> messages) {
        if (!CollectionUtils.isEmpty(messages)) {
            Long receiverId = messages.getFirst().getReceiverId();
            CountDownLatch latch = new CountDownLatch(3);
            //发送人信息
            Thread.ofVirtual().start(() -> {
                defaultMessageAssembleStrategy.assemble(messages);
                latch.countDown();
            });
            //推文信息
            Thread.ofVirtual().start(() -> {
                List<Long> chirperIds = new ArrayList<>();
                messages.forEach(notificationDto -> {
                    try {
                        if (NoticeEntityTypeEnums.CHIRPER.name().equals(notificationDto.getEntityType())) {
                            Long sonEntityId = notificationDto.getSonEntity() != null ? Long.parseLong(notificationDto.getSonEntity()) : null;
                            Long entityId = notificationDto.getEntity() != null ? Long.parseLong(notificationDto.getEntity()) : null;
                            chirperIds.add(sonEntityId);
                            chirperIds.add(entityId);
                        }
                    } catch (Exception e) {
                        log.warn("组装完整站内信，转化实体id时发生错误", e.getCause());
                    }
                });
                if (!chirperIds.isEmpty()) {
                    List<ChirperDto> chirperDtos = chirperClient.getContent(chirperIds, receiverId).getBody();
                    if (chirperDtos != null && !chirperDtos.isEmpty()) {
                        Map<Long, ChirperDto> collect = chirperDtos.stream().collect(Collectors.toMap(ChirperDto::getId, Function.identity()));
                        messages.forEach(notificationDto -> {
                            try {
                                if (NoticeEntityTypeEnums.CHIRPER.name().equals(notificationDto.getEntityType())) {
                                    Long sonEntityId = notificationDto.getSonEntity() != null ? Long.parseLong(notificationDto.getSonEntity()) : null;
                                    Long entityId = notificationDto.getEntity() != null ? Long.parseLong(notificationDto.getEntity()) : null;
                                    notificationDto.setEntity(objectMapper.writeValueAsString(collect.get(entityId)));
                                    notificationDto.setSonEntity(objectMapper.writeValueAsString(collect.get(sonEntityId)));
                                }
                            } catch (JsonProcessingException e) {
                                log.warn("组装完整站内信时发生错误", e.getCause());
                            }
                        });
                    }
                }
                latch.countDown();
            });
            //社群邀请信息
            Thread.ofVirtual().start(() -> {
                List<Long> invitationIdList = new ArrayList<>();
                for (NotificationDto message : messages) {
                    if (NoticeEntityTypeEnums.COMMUNITY_INVITATION.name().equals(message.getEntityType())) {
                        invitationIdList.add(Long.valueOf(message.getSonEntity()));
                    }
                }
                if (!invitationIdList.isEmpty()) {
                    List<InvitationDto> invitationDtos = communityClient.getById(invitationIdList).getBody();
                    if (!CollectionUtils.isEmpty(invitationDtos)) {
                        try {
                            Map<Long, InvitationDto> invitationDtoMap = invitationDtos.stream().collect(Collectors.toMap(InvitationDto::getId, Function.identity()));
                            for (NotificationDto message : messages) {
                                if (NoticeEntityTypeEnums.COMMUNITY_INVITATION.name().equals(message.getEntityType())) {
                                    message.setSonEntity(objectMapper.writeValueAsString(invitationDtoMap.get(Long.valueOf(message.getSonEntity()))));
                                }
                            }
                        } catch (JsonProcessingException e) {
                            log.error("组装完整站内信是出错", e);
                        }
                    }
                }
                latch.countDown();
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new ChirpException(Code.ERR_SYSTEM, "线程中断，服务器异常");
            }
        }
        return messages;
    }
}
