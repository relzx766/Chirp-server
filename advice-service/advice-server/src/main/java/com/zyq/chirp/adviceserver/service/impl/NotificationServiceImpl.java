package com.zyq.chirp.adviceserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.convertor.NoticeConvertor;
import com.zyq.chirp.adviceserver.domain.enums.NoticeStatusEnums;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;
import com.zyq.chirp.adviceserver.mapper.NotificationMapper;
import com.zyq.chirp.adviceserver.service.NotificationService;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.util.StringUtil;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.RelationDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    @Resource
    NotificationMapper notificationMapper;
    @Resource
    NoticeConvertor noticeConvertor;
    @Value("${default-config.page-size}")
    Integer pageSize;
    @Resource
    UserClient userClient;
    @Value("${mq.topic.site-message.notice}")
    String notice;



    /**
     * id由上游mq生成
     *
     */
    @Override
    public void saveBatch(Collection<Notification> notifications) {
        notificationMapper.insertBatch(notifications);
    }

    @Override
    public List<NotificationDto> getSendable(Collection<NotificationDto> notificationDtos) {
        try {
            if (!CollectionUtils.isEmpty(notificationDtos)) {
                Set<String> fromAndToStrList = notificationDtos.stream().map(notificationDto -> StringUtil.combineKey(notificationDto.getSenderId(), notificationDto.getReceiverId())).collect(Collectors.toSet());
                Map<String, RelationDto> relationDtoMap = userClient.getRelation(fromAndToStrList).getBody();
                if (!CollectionUtils.isEmpty(relationDtoMap)) {
                    return notificationDtos.stream().peek(notificationDto -> {
                        RelationDto relationDto = relationDtoMap.get(StringUtil.combineKey(notificationDto.getSenderId(), notificationDto.getReceiverId()));
                        if (relationDto.getIsBlock()) {
                            notificationDto.setStatus(NoticeStatusEnums.UNREACHABLE.getStatus());
                        }
                    }).toList();
                }
                return notificationDtos.stream().toList();
            }
            return List.of();
        } catch (Exception e) {
            log.error("", e);
            throw new ChirpException(Code.ERR_SYSTEM, e);
        }
    }


    @Override
    public List<NotificationDto> getPageByReceiverId(Integer page, Long receiverId) {
        Page<Notification> searchPage = new Page<>(page, pageSize);
        searchPage.setSearchCount(false);
        Page<Notification> notice = notificationMapper.selectPage(searchPage, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .orderByDesc(Notification::getCreateTime)
                .in(Notification::getStatus, NoticeStatusEnums.READ.getStatus(), NoticeStatusEnums.UNREAD.getStatus()));
        if (!notice.getRecords().isEmpty()) {
            return notice.getRecords().stream()
                    .map(notification -> noticeConvertor.pojoToDto(notification))
                    .toList();
        }
        return List.of();
    }

    @Override
    public Integer getUnReadCount(Long receiverId) {
        return Math.toIntExact(notificationMapper.selectCount(new LambdaQueryWrapper<Notification>().eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getStatus, NoticeStatusEnums.UNREAD.getStatus())));
    }

    @Override
    public void readAll(Long receiverId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .set(Notification::getStatus, NoticeStatusEnums.READ.getStatus())
                .eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getStatus, NoticeStatusEnums.UNREAD.getStatus()));
    }



}
