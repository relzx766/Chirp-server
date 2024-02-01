package com.zyq.chirp.chirperserver.aspect;

import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.mq.model.Message;
import com.zyq.chirp.common.util.TextUtil;
import com.zyq.chirp.feedclient.dto.FeedDto;
import com.zyq.chirp.userclient.client.UserClient;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
public class ParseMentionedAspect {
    @Resource
    KafkaTemplate<String, Object> kafkaTemplate;
    @Resource
    UserClient userClient;
    @Value("${mq.topic.site-message.mentioned}")
    String mentionedTopic;
    @Value("${mq.topic.publish}")
    String publishTopic;
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Pointcut("@annotation(com.zyq.chirp.chirperserver.aspect.ParseMentioned)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object doParse(ProceedingJoinPoint joinPoint) {
        try {
            Object result = joinPoint.proceed();
            if (result instanceof ChirperDto chirperDto) {
                sendMentioned(chirperDto);
                sendPublish(chirperDto);
                commitTag(chirperDto);
            }
            if (result instanceof List<?> chirperDtos) {
                parseTend((List<ChirperDto>) chirperDtos);
            }
            return result;
        } catch (Throwable e) {
            throw new ChirpException(Code.ERR_BUSINESS, e);
        }
    }

    public void sendMentioned(ChirperDto chirperDto) {
        Thread.ofVirtual().start(() -> {
            List<String> usernames = TextUtil.findMentioned(chirperDto.getText());
            if (!usernames.isEmpty()) {
                userClient.getIdByUsername(usernames).getBody().forEach(id -> {
                    NotificationDto messageDto = NotificationDto.builder()
                            .senderId(chirperDto.getAuthorId())
                            .receiverId(id)
                            .sonEntity(chirperDto.getId().toString())
                            .build();
                    kafkaTemplate.send(mentionedTopic, messageDto);
                });
            }
        });
    }

    public void sendPublish(ChirperDto chirperDto) {
        Thread.ofVirtual().start(() -> {
            FeedDto feedDto = FeedDto.builder()
                    .publisher(chirperDto.getAuthorId().toString())
                    .contentId(chirperDto.getId().toString())
                    .score((double) chirperDto.getCreateTime().getTime())
                    .build();
            Message<FeedDto> message = new Message<>();
            message.setBody(feedDto);
            kafkaTemplate.send(publishTopic, feedDto.getPublisher(), message);
        });

    }

    public void parseTend(List<ChirperDto> chirperDtos) {
        Thread.ofVirtual().start(() -> {
            List<String> texts = chirperDtos.stream().map(ChirperDto::getText).toList();
            ZSetOperations<String, Object> operations = redisTemplate.opsForZSet();
            TextUtil.findTags(texts).forEach(tag -> {
                operations.incrementScore(CacheKey.TEND_TAG_BOUND_KEY.getKey(), tag, 1);
            });
        });
    }

    public void commitTag(ChirperDto chirperDto) {
        Thread.ofVirtual().start(() -> {
            TextUtil.findTags(chirperDto.getText()).forEach(tag -> {
                ZSetOperations<String, Object> operations = redisTemplate.opsForZSet();
                operations.incrementScore(CacheKey.TEND_TAG_BOUND_KEY.getKey(), tag, 1);
                operations.incrementScore(CacheKey.TEND_POST_BOUND_KEY.getKey(), tag, 1);
            });
        });
    }
}
