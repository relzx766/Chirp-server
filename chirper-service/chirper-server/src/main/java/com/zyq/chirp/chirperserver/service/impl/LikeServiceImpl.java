package com.zyq.chirp.chirperserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyq.chirp.adviceclient.dto.MessageType;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.chirpclient.dto.LikeDto;
import com.zyq.chirp.chirperserver.aspect.Statistic;
import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.chirperserver.domain.enums.LikeType;
import com.zyq.chirp.chirperserver.domain.pojo.Like;
import com.zyq.chirp.chirperserver.mapper.LikeMapper;
import com.zyq.chirp.chirperserver.mq.producer.ChirperProducer;
import com.zyq.chirp.chirperserver.service.LikeService;
import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import com.zyq.chirp.common.util.CacheUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;

@Service
public class LikeServiceImpl implements LikeService {
    @Resource
    LikeMapper likeMapper;

    @Resource
    ChirperProducer<SiteMessageDto> chirperProducer;
    @Resource
    RedisTemplate redisTemplate;
    @Value("${mq.topic.site-message.like}")
    String topic;
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
        if (!flag) {
            throw new ChirpException(Code.ERR_BUSINESS, "重复点赞");
        }
        SiteMessageDto siteMessageDto = new SiteMessageDto(
                likeDto.getUserId(), likeDto.getChirperId(), MessageType.LIKE.name());
        chirperProducer.avoidRedundancySend(CacheUtil.combineKey(likeDto.getChirperId(), likeDto.getUserId()),
                topic, siteMessageDto, Duration.ofHours(expire));
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
