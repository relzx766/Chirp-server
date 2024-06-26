package com.zyq.chirp.feedserver.mq.consumer;

import com.zyq.chirp.common.mq.model.Message;
import com.zyq.chirp.feedclient.dto.FeedDto;
import com.zyq.chirp.feedserver.service.FeedService;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.FollowDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PublishConsumer {
    @Resource
    UserClient userClient;
    @Value("${default-config.follower-query-size}")
    Integer querySize;
    @Value("${mq.retry.max}")
    Integer maxRetryTimes;
    @Resource
    FeedService feedService;
    @Value("${mq.topic.tweeted}")
    String tweetedTopic;
    @Resource
    KafkaTemplate<String, Message<FeedDto>> kafkaTemplate;

    @KafkaListener(topics = "${mq.topic.publish}",
            batch = "false", concurrency = "4")
    public void receiver(@Payload ConsumerRecord<String, Message<FeedDto>> record, Acknowledgment ack) {
        Message<FeedDto> message = record.value();
        try {
            FeedDto feedDto = message.getBody();
            long userId = Long.parseLong(feedDto.getPublisher());
            FollowDto followDto = userClient.getFollowerCount(userId).getBody();
            for (int i = 0; i < Math.ceilDiv(followDto.getFollower(), querySize); i++) {
                int finalI = i;
                Thread.ofVirtual().start(() -> {
                    List<Long> followers = userClient.getFollowerIds(userId, finalI, querySize).getBody();
                    if (followers != null && !followers.isEmpty()) {
                        followers.forEach(follower -> Thread.ofVirtual().start(() -> {
                            try {
                                FeedDto dto = FeedDto.builder()
                                        .receiverId(follower.toString())
                                        .publisher(feedDto.getPublisher())
                                        .contentId(feedDto.getContentId())
                                        .score(feedDto.getScore())
                                        .build();
                                feedService.addOne(dto);
                                //写入后生产消息给下游消费者
                                Message<FeedDto> dtoMessage = Message.<FeedDto>builder().body(dto).retryTimes(0).build();
                                kafkaTemplate.send(tweetedTopic, dtoMessage);
                            } catch (Exception e) {
                                log.error("", e);
                            }
                        }));
                    }
                });
            }
        } catch (Exception e) {
            log.error("推送用户推文更新通知失败,错误==>", e);
            if (message.getRetryTimes() < maxRetryTimes) {
                message.setRetryTimes(message.getRetryTimes() + 1);
                kafkaTemplate.send(record.topic(), record.key(), message);
            }
        } finally {
            ack.acknowledge();
        }
    }
}
