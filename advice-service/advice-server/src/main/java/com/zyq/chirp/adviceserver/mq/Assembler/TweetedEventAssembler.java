package com.zyq.chirp.adviceserver.mq.Assembler;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.domain.enums.NoticeEntityTypeEnums;
import com.zyq.chirp.adviceserver.domain.enums.NoticeEventTypeEnums;
import com.zyq.chirp.adviceserver.domain.enums.NoticeTypeEnums;
import com.zyq.chirp.authclient.client.AuthClient;
import com.zyq.chirp.common.mq.model.Message;
import com.zyq.chirp.feedclient.dto.FeedDto;
import com.zyq.chirp.userclient.client.UserClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * 消费推文发布消息，组装为完整的推文发布事件，推送给登录用户
 */
@Component
@Slf4j
public class TweetedEventAssembler {
    @Value("${default-config.follower-query-size}")
    Integer querySize;
    @Resource
    KafkaTemplate<String, NotificationDto> kafkaTemplate;
    @Resource
    UserClient userClient;
    @Resource
    AuthClient authClient;
    @Value("${mq.topic.site-message.tweeted-advice}")
    String tweeted;

    @KafkaListener(topics = "${mq.topic.tweeted}",
            groupId = "${mq.consumer.group.tweeted}",
            batch = "false", concurrency = "4")
    public void receiver(@Payload ConsumerRecord<String, Message<FeedDto>> record, Acknowledgment ack) {
        try {
            Message<FeedDto> message = record.value();
            FeedDto feedDto = message.getBody();
            long userId = Long.parseLong(feedDto.getPublisher());
            Long followerCount = userClient.getFollowerCount(userId).getBody();
            for (int i = 0; i < Math.ceilDiv(followerCount, querySize); i++) {
                int finalI = i;
                Thread.ofVirtual().start(() -> {
                    List<Long> followers = userClient.getFollowerIds(userId, finalI, querySize).getBody();
                    if (followers != null) {
                        List<String> followerIds = followers.stream().map(String::valueOf).toList();
                        Map<String, Boolean> onlineMap = authClient.multiCheck(followerIds).getBody();
                        if (onlineMap != null) {
                            onlineMap.forEach((follower, onlineStatus) -> {
                                //在线就推送
                                if (onlineStatus) {
                                    NotificationDto messageDto = NotificationDto.builder()
                                            .id(IdWorker.getId())
                                            .receiverId(Long.parseLong(follower))
                                            .senderId(Long.parseLong(feedDto.getPublisher()))
                                            .sonEntity(feedDto.getContentId())
                                            .entityType(NoticeEntityTypeEnums.CHIRPER.name())
                                            .event(NoticeEventTypeEnums.TWEETED.name())
                                            .createTime(new Timestamp(System.currentTimeMillis()))
                                            .noticeType(NoticeTypeEnums.SYSTEM.name())
                                            .build();
                                    kafkaTemplate.send(tweeted, messageDto);
                                }
                            });
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.error("组装发推通知错误，错误=>", e);
        } finally {
            ack.acknowledge();
        }
    }
}
