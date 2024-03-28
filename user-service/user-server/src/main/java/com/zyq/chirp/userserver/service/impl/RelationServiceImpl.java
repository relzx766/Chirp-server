package com.zyq.chirp.userserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.mq.model.Message;
import com.zyq.chirp.common.util.StringUtil;
import com.zyq.chirp.userclient.dto.RelationDto;
import com.zyq.chirp.userserver.convertor.RelationConvertor;
import com.zyq.chirp.userserver.mapper.RelationMapper;
import com.zyq.chirp.userserver.model.enumeration.RelationType;
import com.zyq.chirp.userserver.model.pojo.Relation;
import com.zyq.chirp.userserver.service.RelationService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "relation")
public class RelationServiceImpl implements RelationService {
    @Resource
    RelationMapper relationMapper;
    @Resource
    RelationConvertor relationConvertor;
    @Value("${mq.topic.site-message.follow}")
    String follow;
    @Value("${mq.topic.unfollow}")
    String unfollowTopic;
    Integer expire = 6;
    @Resource
    KafkaTemplate<String, Object> kafkaTemplate;
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Override
    public Relation getRelationType(Long fromId, Long toId) {
        if (Objects.isNull(fromId) || Objects.isNull(toId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "信息不完善,无法获取用户关系");
        }
        return relationMapper.selectOne(new LambdaQueryWrapper<Relation>()
                .select(Relation::getStatus)
                .eq(Relation::getFromId, fromId)
                .eq(Relation::getToId, toId));
    }

    @Override
    public List<RelationDto> getUserRelation(Collection<Long> userIds, Long targetUserId) {
        if (targetUserId != null && userIds != null && !userIds.isEmpty()) {
            ArrayList<Long> noRecordUser = new ArrayList<>(userIds);
            List<RelationDto> records = new ArrayList<>(relationMapper.selectList(new LambdaQueryWrapper<Relation>()
                            .eq(Relation::getFromId, targetUserId)
                            .in(Relation::getToId, userIds))
                    .stream()
                    .map(r -> {
                        noRecordUser.remove(r.getToId());
                        return relationConvertor.pojoToDto(r);
                    })
                    .toList());
            noRecordUser.forEach(id -> {
                records.add(RelationDto.unFollow(id, targetUserId));
            });
            return records;
        }
        return List.of();
    }

    @Override
    public Map<String, RelationDto> getUserRelation(Collection<String> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return Map.of();
        }
        List<Relation> fromAndToList = userList.stream().map(fromAndToStr -> {
            String[] fromAndTo = StringUtil.divideKey(fromAndToStr);
            return Relation.builder().fromId(Long.valueOf(fromAndTo[0])).toId(Long.valueOf(fromAndTo[1])).build();
        }).toList();
        Map<String, RelationDto> relationDtoMap = relationMapper.getList(fromAndToList)
                .stream()
                .map(relation -> relationConvertor.pojoToDto(relation))
                .collect(Collectors.toMap(r -> StringUtil.combineKey(r.getFromId(), r.getToId()), Function.identity()));
        return userList.stream().map(fromAndTo -> {
            RelationDto relationDto = relationDtoMap.get(fromAndTo);
            if (relationDto == null) {
                String[] divided = StringUtil.divideKey(fromAndTo);
                relationDto = RelationDto.builder().fromId(Long.valueOf(divided[0])).toId(Long.valueOf(divided[1])).status(RelationType.UNFOLLOWED.getRelation()).build();
            }
            return Map.entry(fromAndTo, relationDto);
        }).collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue(), (k1, k2) -> k1));
    }

    @Override
    public List<RelationDto> getUserRelationReverse(Collection<Long> userIds, Long targetUserId) {
        if (targetUserId != null && userIds != null && !userIds.isEmpty()) {

            ArrayList<Long> noRecordUser = new ArrayList<>(userIds);
        List<RelationDto> records = new ArrayList<>(relationMapper.selectList(new LambdaQueryWrapper<Relation>()
                        .eq(Relation::getToId, targetUserId)
                        .in(Relation::getFromId, userIds))
                .stream()
                .map(r -> {
                    noRecordUser.remove(r.getFromId());
                    return relationConvertor.pojoToDto(r);
                })
                .toList());
        noRecordUser.forEach(id -> {
            records.add(RelationDto.unFollow(id, targetUserId));
        });
        return records;
        }
        return List.of();
    }

    @Override
    public List<Long> getFollower(Long userId, Integer page, Integer pageSize) {
        Page<Relation> selectPage = new Page<>(page, pageSize);
        selectPage.setSearchCount(false);
        return relationMapper.selectPage(selectPage, new LambdaQueryWrapper<Relation>()
                        .select(Relation::getFromId)
                        .eq(Relation::getToId, userId)
                        .eq(Relation::getStatus, RelationType.FOLLOWING.getRelation()))
                .getRecords()
                .stream()
                .map(Relation::getFromId)
                .toList();
    }

    @Override
    public List<Long> getFollowing(Long userId) {
        return relationMapper.selectList(new LambdaQueryWrapper<Relation>()
                        .select(Relation::getToId)
                        .eq(Relation::getFromId, userId))
                .stream()
                .map(Relation::getToId)
                .toList();
    }

    @Override
    public List<Long> getFollowing(Long userId, Integer page, Integer pageSize) {
        Page<Relation> selectPage = new Page<>(page, pageSize);
        selectPage.setSearchCount(false);
        return relationMapper.selectPage(selectPage, new LambdaQueryWrapper<Relation>()
                        .select(Relation::getToId)
                        .eq(Relation::getFromId, userId)
                        .eq(Relation::getStatus, RelationType.FOLLOWING.getRelation()))
                .getRecords()
                .stream()
                .map(Relation::getToId)
                .toList();
    }

    @Override
    public Long getFollowerCount(Long userId) {
        return relationMapper.selectCount(new LambdaQueryWrapper<Relation>()
                .eq(Relation::getToId, userId).eq(Relation::getStatus, RelationType.FOLLOWING.getRelation()));
    }

    @Override
    public Long getFollowingCount(Long userId) {
        return relationMapper.selectCount(new LambdaQueryWrapper<Relation>()
                .eq(Relation::getFromId, userId).eq(Relation::getStatus, RelationType.FOLLOWING.getRelation()));
    }


    @Override
    @Cacheable(key = "#fromId+':'+#toId")
    public void follow(Long fromId, Long toId) {
        if (Objects.isNull(fromId) || Objects.isNull(toId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "信息不完善");
        } else if (fromId.equals(toId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "不能关注自己");
        }
        Relation relation = new Relation(fromId, toId,
                new Timestamp(System.currentTimeMillis()),
                RelationType.FOLLOWING.getRelation());
        relationMapper.replace(relation);
                NotificationDto messageDto = NotificationDto.builder()
                        .receiverId(toId)
                        .senderId(fromId)
                        .build();
                kafkaTemplate.send(follow, messageDto);
    }

    @Override
    @CacheEvict(key = "#fromId+':'+#toId")
    public void unfollow(Long fromId, Long toId) {
        if (Objects.isNull(fromId) || Objects.isNull(toId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "信息不完善");
        }
        Relation relation = Relation.builder()
                .fromId(fromId)
                .toId(toId)
                .status(RelationType.FOLLOWING.getRelation())
                .build();
        relationMapper.delete(new LambdaQueryWrapper<Relation>()
                .eq(Relation::getFromId, relation.getFromId())
                .eq(Relation::getToId, relation.getToId())
                .eq(Relation::getStatus, relation.getStatus()));
        Message<RelationDto> message = new Message<>();
        message.setBody(relationConvertor.pojoToDto(relation));
        kafkaTemplate.send(unfollowTopic, message);
    }

    @Override
    public void block(Long fromId, Long toId) {
        if (Objects.isNull(fromId) || Objects.isNull(toId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "信息不完善");
        } else if (fromId.equals(toId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "不能拉黑自己");
        }
        Relation relation = new Relation(fromId, toId, new Timestamp(System.currentTimeMillis()), RelationType.BLOCK.getRelation());
        //将用户关系替换为block
        relationMapper.replace(relation);
        //如果to关注了from，也删除该记录
        relationMapper.delete(new LambdaUpdateWrapper<Relation>().eq(Relation::getFromId, toId).eq(Relation::getToId, fromId).eq(Relation::getStatus, RelationType.FOLLOWING.getRelation()));
    }

    @Override
    public void cancelBlock(Long fromId, Long toId) {
        if (Objects.isNull(fromId) || Objects.isNull(toId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "信息不完善");
        }
        relationMapper.delete(new LambdaQueryWrapper<Relation>()
                .eq(Relation::getFromId, fromId)
                .eq(Relation::getToId, toId)
                .eq(Relation::getStatus, RelationType.BLOCK.getRelation()));
    }
}
