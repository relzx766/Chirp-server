package com.zyq.chirp.chirperserver.aspect;

import com.zyq.chirp.adviceclient.dto.EntityType;
import com.zyq.chirp.adviceclient.dto.EventType;
import com.zyq.chirp.adviceclient.dto.NoticeType;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import com.zyq.chirp.common.util.TextUtil;
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
import java.util.concurrent.CompletableFuture;

@Aspect
@Component
public class ParseMentionedAspect {
    @Resource
    KafkaTemplate<String, Object> kafkaTemplate;
    @Resource
    UserClient userClient;
    @Value("${mq.topic.site-message.mentioned}")
    String mentionedTopic;
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Pointcut("@annotation(com.zyq.chirp.chirperserver.aspect.ParseMentioned)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object doParse(ProceedingJoinPoint joinPoint) {
        try {
            Object result = joinPoint.proceed();
            CompletableFuture.runAsync(() -> {
                if (result instanceof ChirperDto chirperDto) {
                    sendMentioned(chirperDto);
                    commitTag(chirperDto);
                }
                if (result instanceof List<?> chirperDtos) {
                    parseTend((List<ChirperDto>) chirperDtos);
                }
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
            return result;
        } catch (Throwable e) {
            throw new ChirpException(Code.ERR_BUSINESS, e);
        }
    }

    public void sendMentioned(ChirperDto chirperDto) {
        List<String> usernames = TextUtil.findMentioned(chirperDto.getText());
        if (!usernames.isEmpty()) {
            userClient.getIdByUsername(usernames).getBody().forEach(id -> {
                SiteMessageDto messageDto = SiteMessageDto.builder()
                        .senderId(chirperDto.getAuthorId())
                        .receiverId(id)
                        .event(EventType.MENTIONED.name())
                        .entityType(EntityType.CHIRPER.name())
                        .noticeType(NoticeType.USER.name())
                        .sonEntity(chirperDto.getId().toString())
                        .build();
                kafkaTemplate.send(mentionedTopic, messageDto);
            });
        }
    }

    public void parseTend(List<ChirperDto> chirperDtos) {
        List<String> texts = chirperDtos.stream().map(ChirperDto::getText).toList();
        ZSetOperations<String, Object> operations = redisTemplate.opsForZSet();
        TextUtil.findTags(texts).forEach(tag -> {
            operations.incrementScore(CacheKey.TEND_TAG_BOUND_KEY.getKey(), tag, 1);
        });
    }

    public void commitTag(ChirperDto chirperDto) {
        TextUtil.findTags(chirperDto.getText()).forEach(tag -> {
            ZSetOperations<String, Object> operations = redisTemplate.opsForZSet();
            operations.incrementScore(CacheKey.TEND_TAG_BOUND_KEY.getKey(), tag, 1);
            operations.incrementScore(CacheKey.TEND_POST_BOUND_KEY.getKey(), tag, 1);
        });

    }
}
