package com.zyq.chirp.chirperserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirpclient.dto.LikeDto;
import com.zyq.chirp.chirperserver.aspect.Statistic;
import com.zyq.chirp.chirperserver.domain.enums.ActionTypeEnums;
import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.chirperserver.domain.pojo.Like;
import com.zyq.chirp.chirperserver.mapper.LikeMapper;
import com.zyq.chirp.chirperserver.service.ChirperService;
import com.zyq.chirp.chirperserver.service.LikeService;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.mq.enums.DefaultOperation;
import com.zyq.chirp.common.mq.model.Action;
import com.zyq.chirp.common.util.RetryUtil;
import com.zyq.chirp.common.util.StringUtil;
import com.zyq.chirp.communityclient.client.CommunityClient;
import com.zyq.chirp.communityclient.dto.CommunityDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@CacheConfig(cacheNames = "like:chirper")
public class LikeServiceImpl implements LikeService {
    @Resource
    LikeMapper likeMapper;

    @Resource
    KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${mq.topic.site-message.like}")
    String topic;
    @Value("${default-config.page-size}")
    Integer pageSize;
    Integer expire = 6;
    @Value("${mq.topic.chirper.like.count}")
    String LIKE_INCREMENT_COUNT_TOPIC;
    @Value("${mq.topic.chirper.like.record}")
    String LIKE_RECORD_TOPIC;

    @Override
    @Statistic(id = "#likeDto.chirperId", key = {CacheKey.VIEW_COUNT_BOUND_KEY})
    @Cacheable(key = "#likeDto.userId+':'+#likeDto.chirperId")
    public void addLike(LikeDto likeDto) {
        if (likeDto.getUserId() == null || likeDto.getChirperId() == null) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供用户或推文信息");
        }
        Action<Long, Long> action = new Action<>(
                ActionTypeEnums.LIKE.getAction(),
                DefaultOperation.INSET.getOperation(),
                likeDto.getUserId(),
                likeDto.getChirperId(),
                System.currentTimeMillis()
        );
        kafkaTemplate.send(LIKE_RECORD_TOPIC, action);

    }

    @Override
    public void saveLike(List<Action<Long, Long>> actions) {
        List<Like> likes = actions.stream().map(action -> new Like(
                action.getTarget(),
                action.getOperator(),
                Timestamp.valueOf(LocalDateTime.now()))).toList();
        try {
            RetryUtil.doDBRetry(() -> likeMapper.insertList(likes));
            Map<Long, Long> likeCount = likes.stream().collect(Collectors.groupingBy(Like::getChirperId, Collectors.counting()));
            likeCount.forEach((chirperId, count) -> {
                Action<Long, Long> increLikeAction = new Action<>();
                increLikeAction.setTarget(chirperId);
                increLikeAction.setActionType(ActionTypeEnums.LIKE.getAction());
                increLikeAction.setOperation(DefaultOperation.INCREMENT.getOperation());
                increLikeAction.setActionTime(System.currentTimeMillis());
                increLikeAction.setMulti(Math.toIntExact(count));
                kafkaTemplate.send(LIKE_INCREMENT_COUNT_TOPIC, increLikeAction);
            });

            actions.forEach(action -> {
                NotificationDto notificationDto = NotificationDto.builder()
                        .sonEntity(String.valueOf(action.getTarget()))
                        .senderId(action.getOperator())
                        .build();
                kafkaTemplate.send(topic, notificationDto);
            });
        } catch (ExecutionException e) {
            log.error("插入点赞时发生无法成功的错误，点赞信息:{},错误:", likes, e);
        } catch (Exception e) {
            log.error("插入点赞失败，点赞消息:{},错误:", likes, e);
            actions.forEach(action -> {
                log.info("尝试重发:{}", action);
                kafkaTemplate.send(LIKE_RECORD_TOPIC, action);
            });
        }
    }

    @Override
    @CacheEvict(key = "#likeDto.userId+':'+#likeDto.chirperId")
    public void cancelLike(LikeDto likeDto) {
        if (likeDto.getUserId() == null || likeDto.getChirperId() == null) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供用户或推文信息");
        }
        Action<Long, Long> action = new Action<>(
                ActionTypeEnums.LIKE.getAction(),
                DefaultOperation.DELETE.getOperation(),
                likeDto.getUserId(),
                likeDto.getChirperId(),
                System.currentTimeMillis()
        );
        kafkaTemplate.send(LIKE_RECORD_TOPIC, action);
    }

    @Override
    public void saveLikeCancel(List<Action<Long, Long>> actions) {
        Map<Long, List<Like>> collect = actions.stream().map(action ->
                        new Like(action.getTarget(), action.getOperator(), new Timestamp(action.getActionTime())))
                .collect(Collectors.groupingBy(Like::getChirperId));

        collect.forEach((chirperId, likes) -> {
            try {
                var ref = new Object() {
                    int affectRows = 0;
                };
                RetryUtil.doDBRetry(() ->
                {
                    ref.affectRows = likeMapper.deleteList(likes);
                    return true;
                });
                if (ref.affectRows > 0) {
                    Action<Long, Long> action = new Action<>(
                            ActionTypeEnums.LIKE.getAction(),
                            DefaultOperation.DECREMENT.getOperation(),
                            ref.affectRows,
                            null,
                            chirperId,
                            System.currentTimeMillis()
                    );
                    kafkaTemplate.send(LIKE_INCREMENT_COUNT_TOPIC, action);
                }
            } catch (ExecutionException e) {
                log.error("删除点赞时发生无法成功的错误，点赞信息:{},错误:", likes, e);
            } catch (Exception e) {
                log.error("删除点赞失败，点赞消息:{},错误:", likes, e);
                likes.forEach(like -> {
                    Action<Long, Long> action = new Action<>(
                            ActionTypeEnums.LIKE.getAction(),
                            DefaultOperation.DELETE.getOperation(),
                            like.getUserId(),
                            like.getChirperId(),
                            System.currentTimeMillis()
                    );
                    log.warn("尝试重发:{}", action);
                    kafkaTemplate.send(LIKE_RECORD_TOPIC, action);
                });
            }
        });

    }

    @Override
    public void modifyLikeCount(List<Action<Long, Long>> actions) {
        Map<Long, List<Action<Long, Long>>> collect =
                actions.stream().collect(Collectors.groupingBy(Action::getTarget));
        collect.forEach((chirperId, actionList) -> {
            int count = Action.getIncCount(actionList);
            try {
                RetryUtil.doDBRetry(() -> likeMapper.updateChirperLikeCount(chirperId, count));
            } catch (ExecutionException e) {
                log.error("修改点赞数时发生无法成功的错误，推文id:{}", chirperId, e);
            } catch (Exception e) {
                log.error("修改点赞数失败,推文id:{}，数量:{},错误:", chirperId, count, e);
                for (Action<Long, Long> action : actionList) {
                    log.warn("尝试重发:{}", action);
                    kafkaTemplate.send(LIKE_INCREMENT_COUNT_TOPIC, action);
                }
            }
        });
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
        Page<Like> likePage = new Page<>(page, pageSize);
        likePage.setSearchCount(false);
        return likeMapper.selectPage(likePage, new LambdaQueryWrapper<Like>()
                        .eq(Like::getUserId, userId)
                        .orderByDesc(Like::getCreateTime))
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
