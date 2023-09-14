package com.zyq.chirp.adviceserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.convertor.MessageConvertor;
import com.zyq.chirp.adviceserver.domain.pojo.InteractionMessage;
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
     * @param interactionMessage
     */
    @Override
    public void save(InteractionMessage interactionMessage) {
        noticeMessageMapper.insertOne(interactionMessage);
    }

    /**
     * id由上游mq生成
     *
     * @param interactionMessages
     */
    @Override
    public void saveBatch(Collection<InteractionMessage> interactionMessages) {
        System.out.println("插入消息到数据库....");
        System.out.println(interactionMessages);
        noticeMessageMapper.insertBatch(interactionMessages);
    }

    @Override
    public List<SiteMessageDto> getByReceiverId(Long receiverId) {
        return noticeMessageMapper.getByReceiverId(receiverId).stream()
                .map(message -> messageConvertor.pojoToDto(message))
                .toList();
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
        noticeMessageMapper.update(null, new LambdaUpdateWrapper<InteractionMessage>()
                .eq(InteractionMessage::getId, messageId)
                .eq(InteractionMessage::getReceiverId, receiverId)
                .set(InteractionMessage::getIsRead, true));
    }

    @Override
    public void markAsRead(Collection<Long> messageIds, Long receiverId) {
        noticeMessageMapper.update(null, new LambdaUpdateWrapper<InteractionMessage>()
                .eq(InteractionMessage::getReceiverId, receiverId)
                .in(InteractionMessage::getId, messageIds)
                .set(InteractionMessage::getIsRead, true));
    }
}
