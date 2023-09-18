package com.zyq.chirp.adviceserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.convertor.MessageConvertor;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;
import com.zyq.chirp.adviceserver.mapper.NoticeMessageMapper;
import com.zyq.chirp.adviceserver.service.InteractionMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class InteractionMessageImpl implements InteractionMessageService {
    @Resource
    NoticeMessageMapper noticeMessageMapper;
    @Resource
    MessageConvertor messageConvertor;

    /**
     * id由上游mq生成
     *
     * @param notification
     */
    @Override
    public void save(Notification notification) {
        noticeMessageMapper.insert(notification);
    }

    /**
     * id由上游mq生成
     *
     * @param notifications
     */
    @Override
    public void saveBatch(Collection<Notification> notifications) {
        noticeMessageMapper.insertBatch(notifications);
    }

    @Override
    public List<SiteMessageDto> getByReceiverId(Long receiverId) {
        return noticeMessageMapper.selectList(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, receiverId)
                        .orderByDesc(Notification::getCreateTime))
                .stream()
                .map(notification -> messageConvertor.pojoToDto(notification)).toList();
    }

    @Override
    public List<SiteMessageDto> getPageByReceiverId(Integer page, Long receiverId) {
        return null;
    }

    @Override
    public List<SiteMessageDto> getUnReadByReceiverId(Long receiverId) {
        return noticeMessageMapper.getUnReadByReceiverId(receiverId).stream()
                .map(message -> messageConvertor.pojoToDto(message))
                .toList();
    }

    @Override
    public void markAsRead(Long messageId, Long receiverId) {
        noticeMessageMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, messageId)
                .eq(Notification::getReceiverId, receiverId)
                .set(Notification::getIsRead, true));
    }

    @Override
    public void markAsRead(Collection<Long> messageIds, Long receiverId) {
        noticeMessageMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .in(Notification::getId, messageIds)
                .set(Notification::getIsRead, true));
    }
}
