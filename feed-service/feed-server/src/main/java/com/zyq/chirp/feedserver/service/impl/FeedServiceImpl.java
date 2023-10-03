package com.zyq.chirp.feedserver.service.impl;

import com.zyq.chirp.common.util.PageUtil;
import com.zyq.chirp.feedclient.dto.FeedDto;
import com.zyq.chirp.feedserver.domain.enums.CacheKey;
import com.zyq.chirp.feedserver.service.FeedService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class FeedServiceImpl implements FeedService {
    @Resource
    RedisTemplate<String, Object> redisTemplate;
    @Value("${default-config.page-size}")
    Integer pageSize;

    @Override
    public void addOne(FeedDto feedDto) {
        ZSetOperations<String, Object> operations = redisTemplate.opsForZSet();
        operations.add(STR. "\{ CacheKey.FEED_BOUND_KEY.getKey() }:\{ feedDto.getReceiverId() }" , feedDto.getContentId(), feedDto.getScore());
    }

    @Override
    public void addFeedBatch(Collection<FeedDto> feedDtos) {
        ZSetOperations<String, Object> operations = redisTemplate.opsForZSet();
        for (FeedDto feedDto : feedDtos) {
            try {
                operations.add(STR. "\{ CacheKey.FEED_BOUND_KEY.getKey() }:\{ feedDto.getReceiverId() }" , feedDto.getContentId(), System.currentTimeMillis());
            } catch (Exception e) {
                log.warn("{}", e);
            }
        }
    }


    @Override
    public void removeBatch(String receiverId, Collection<String> contentIds) {
        ZSetOperations<String, Object> operations = redisTemplate.opsForZSet();
        for (String id : contentIds) {
            try {
                operations.remove(STR. "\{ CacheKey.FEED_BOUND_KEY.getKey() }:\{ receiverId }" , id);
            } catch (Exception e) {
                log.warn("{}", e);
            }
        }
    }

    @Override
    public void removeBatch(Collection<FeedDto> feedDtos) {
        ZSetOperations<String, Object> operations = redisTemplate.opsForZSet();
        feedDtos.forEach(feedDto -> {
            try {
                operations.remove(STR. "\{ CacheKey.FEED_BOUND_KEY.getKey() }:\{ feedDto.getReceiverId() }" , feedDto.getContentId());
            } catch (Exception e) {
                log.warn("{}", e);
            }
        });
    }

    @Override
    public Collection<FeedDto> getPage(String receiverId, Integer page) {
        int offset = PageUtil.getOffset(page, pageSize);
        ZSetOperations<String, Object> operations = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> feeds = operations.reverseRangeWithScores(STR. "\{ CacheKey.FEED_BOUND_KEY.getKey() }:\{ receiverId }" , offset, offset + pageSize);
        return feeds != null && !feeds.isEmpty() ? feeds.stream()
                .map(tuple -> FeedDto.builder()
                        .receiverId(receiverId)
                        .contentId((String) tuple.getValue())
                        .score(tuple.getScore())
                        .build())
                .toList() : List.of();

    }

    @Override
    public Collection<FeedDto> getRange(String receiverId, Double start, Double end) {
        ZSetOperations<String, Object> operations = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> feeds = operations.rangeByScoreWithScores(STR. "\{ CacheKey.FEED_BOUND_KEY.getKey() }:\{ receiverId }" , start, end);
        return feeds != null && !feeds.isEmpty() ? feeds.stream()
                .map(tuple -> FeedDto.builder()
                        .receiverId(receiverId)
                        .contentId((String) tuple.getValue())
                        .score(tuple.getScore())
                        .build())
                .toList() : List.of();

    }
}
