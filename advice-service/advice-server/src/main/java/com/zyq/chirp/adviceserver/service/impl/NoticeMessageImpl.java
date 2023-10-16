package com.zyq.chirp.adviceserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.convertor.MessageConvertor;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;
import com.zyq.chirp.adviceserver.mapper.NoticeMessageMapper;
import com.zyq.chirp.adviceserver.service.NoticeMessageService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class NoticeMessageImpl implements NoticeMessageService {
    @Resource
    NoticeMessageMapper noticeMessageMapper;
    @Resource
    MessageConvertor messageConvertor;
    @Value("${default-config.page-size}")
    Integer pageSize;
    @Resource
    KafkaProperties kafkaProperties;
    @Value("${mq.topic.site-message.notice}")
    String notice;


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
        Page<Notification> searchPage = new Page<>(page, pageSize);
        searchPage.setSearchCount(false);
        Page<Notification> notice = noticeMessageMapper.selectPage(searchPage, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .orderByDesc(Notification::getCreateTime));
        if (!notice.getRecords().isEmpty()) {
            return notice.getRecords().stream()
                    .map(notification -> messageConvertor.pojoToDto(notification))
                    .toList();
        }
        return List.of();
    }

    @Override
    public Integer getUnReadCount(Long receiverId) {
        return Math.toIntExact(noticeMessageMapper.selectCount(new LambdaQueryWrapper<Notification>().eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsRead, false)));
    }

    @Override
    public void readAll(Long receiverId) {
        noticeMessageMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .set(Notification::getIsRead, true)
                .eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsRead, false));
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
