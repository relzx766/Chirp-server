package com.zyq.chirp.adviceserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.convertor.NoticeConvertor;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;
import com.zyq.chirp.adviceserver.mapper.NotificationMapper;
import com.zyq.chirp.adviceserver.service.NotificationService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    @Resource
    NotificationMapper notificationMapper;
    @Resource
    NoticeConvertor noticeConvertor;
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
        notificationMapper.insert(notification);
    }

    /**
     * id由上游mq生成
     *
     * @param notifications
     */
    @Override
    public void saveBatch(Collection<Notification> notifications) {
        notificationMapper.insertBatch(notifications);
    }

    @Override
    public List<NotificationDto> getByReceiverId(Long receiverId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, receiverId)
                        .orderByDesc(Notification::getCreateTime))
                .stream()
                .map(notification -> noticeConvertor.pojoToDto(notification)).toList();
    }

    @Override
    public List<NotificationDto> getPageByReceiverId(Integer page, Long receiverId) {
        Page<Notification> searchPage = new Page<>(page, pageSize);
        searchPage.setSearchCount(false);
        Page<Notification> notice = notificationMapper.selectPage(searchPage, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .orderByDesc(Notification::getCreateTime));
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
                .eq(Notification::getIsRead, false)));
    }

    @Override
    public void readAll(Long receiverId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .set(Notification::getIsRead, true)
                .eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsRead, false));
    }

    @Override
    public List<NotificationDto> getUnReadByReceiverId(Long receiverId) {
        return notificationMapper.getUnReadByReceiverId(receiverId).stream()
                .map(message -> noticeConvertor.pojoToDto(message))
                .toList();
    }

    @Override
    public void markAsRead(Long messageId, Long receiverId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, messageId)
                .eq(Notification::getReceiverId, receiverId)
                .set(Notification::getIsRead, true));
    }

    @Override
    public void markAsRead(Collection<Long> messageIds, Long receiverId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .in(Notification::getId, messageIds)
                .set(Notification::getIsRead, true));
    }


}
