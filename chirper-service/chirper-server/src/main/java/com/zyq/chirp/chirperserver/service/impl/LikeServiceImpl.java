package com.zyq.chirp.chirperserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceclient.enums.EntityType;
import com.zyq.chirp.adviceclient.enums.EventType;
import com.zyq.chirp.chirpclient.dto.LikeDto;
import com.zyq.chirp.chirperserver.aspect.Statistic;
import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.chirperserver.domain.enums.LikeType;
import com.zyq.chirp.chirperserver.domain.pojo.Like;
import com.zyq.chirp.chirperserver.mapper.LikeMapper;
import com.zyq.chirp.chirperserver.service.LikeService;
import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import com.zyq.chirp.common.util.CacheUtil;
import com.zyq.chirp.common.util.PageUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

@Service
public class LikeServiceImpl implements LikeService {
    @Resource
    LikeMapper likeMapper;

    @Resource
    KafkaTemplate<String, Object> kafkaTemplate;
    @Resource
    RedisTemplate<String, Object> redisTemplate;
    @Value("${mq.topic.site-message.like}")
    String topic;
    @Value("${default-config.page-size}")
    Integer pageSize;
    Integer expire = 6;

    @Override
    @Statistic(id = "#likeDto.chirperId", key = {CacheKey.VIEW_COUNT_BOUND_KEY, CacheKey.LIKE_COUNT_BOUND_KEY})
    public void addLike(LikeDto likeDto) {
        if (likeDto.getUserId() == null || likeDto.getChirperId() == null) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供用户或推文信息");
        }
        likeDto.setType(LikeType.ADD.getType());
        likeDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
        BoundHashOperations<String, String, LikeDto> operations = redisTemplate.boundHashOps(CacheKey.LIKE_INFO_BOUND_KEY.getKey());
        //在缓存点赞前，需要先删除缓存中取消点赞的记录
        operations.delete(CacheUtil.combineKey(likeDto.getChirperId(), likeDto.getUserId(), LikeType.CANCEL.getType()));
        String key = CacheUtil.combineKey(likeDto.getChirperId(), likeDto.getUserId(), LikeType.ADD.getType());
        Boolean flag = operations.putIfAbsent(key, likeDto);
        if (Boolean.FALSE.equals(flag)) {
            throw new ChirpException(Code.ERR_BUSINESS, "重复点赞");
        }
        Thread.ofVirtual().start(() -> {
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(STR. "\{ EventType.LIKE.name() }:\{ likeDto.getUserId() }:\{ likeDto.getChirperId() }" , 1, Duration.ofHours(expire));
            if (Boolean.TRUE.equals(absent)) {
                NotificationDto notificationDto = NotificationDto.builder()
                        .sonEntity(String.valueOf(likeDto.getChirperId()))
                        .event(EventType.LIKE.name())
                        .entityType(EntityType.CHIRPER.name())
                        .senderId(likeDto.getUserId())
                        .build();
                kafkaTemplate.send(topic, notificationDto);
            }

        });
    }

    @Override
    @Statistic(id = "#likeDto.chirperId", delta = -1, key = CacheKey.LIKE_COUNT_BOUND_KEY)
    public void cancelLike(LikeDto likeDto) {
        if (likeDto.getUserId() == null || likeDto.getChirperId() == null) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供用户或推文信息");
        }
        likeDto.setType(LikeType.CANCEL.getType());
        BoundHashOperations<String, String, LikeDto> operations = redisTemplate.boundHashOps(CacheKey.LIKE_INFO_BOUND_KEY.getKey());
        //取消点赞缓存前，需删除缓存中对应点赞记录
        operations.delete(CacheUtil.combineKey(likeDto.getChirperId(), likeDto.getUserId(), LikeType.ADD.getType()));
        String key = CacheUtil.combineKey(likeDto.getChirperId(), likeDto.getUserId(), LikeType.CANCEL.getType());
        Boolean flag = operations.putIfAbsent(key, likeDto);
        if (!flag) {
            throw new ChirpException(Code.ERR_BUSINESS, "重复取消点赞");
        }
    }

    @Override
    public List<Long> getLikeInfo(Collection<Long> chirperIds, Long userId) {
        if (chirperIds == null || chirperIds.isEmpty() || userId == null) {
            return List.of();
        }
        return likeMapper.selectList(new LambdaQueryWrapper<Like>()
                        .select(Like::getChirperId)
                        .eq(Like::getUserId, userId)
                        .in(Like::getChirperId, chirperIds))
                .stream()
                .map(Like::getChirperId).toList();
    }

    @Override
    public List<Like> getLikeRecord(Long userId, Integer page) {
        int offset = PageUtil.getOffset(page, pageSize);
        return likeMapper.selectPage(new Page<>(offset, pageSize), new LambdaQueryWrapper<Like>()
                        .eq(Like::getUserId, userId))
                .getRecords();
    }


    @Override
    public String getKey(LikeDto likeDto) {
        return likeDto.getUserId() + ":" + likeDto.getChirperId();
    }

    @Override
    public boolean addList(List<Like> likes) {
        return likeMapper.insertList(likes) > 0;
    }

    @Override
    public boolean deleteList(List<Like> likes) {
        return likeMapper.deleteList(likes) > 0;
    }

    @Override
    public int deleteByChirperId(List<Long> chirperIds) {
        return likeMapper.delete(new LambdaQueryWrapper<Like>()
                .in(Like::getChirperId, chirperIds));
    }

    @Override
    public boolean updateLikeCount(Long chirperId, Integer delta) {
        return likeMapper.updateChirperLikeCount(chirperId, delta) > 0;
    }
}
