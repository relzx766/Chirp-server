package com.zyq.chirp.userserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyq.chirp.adviceclient.dto.EntityType;
import com.zyq.chirp.adviceclient.dto.EventType;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import com.zyq.chirp.common.mq.DefaultKafkaProducer;
import com.zyq.chirp.userserver.convertor.RelationConvertor;
import com.zyq.chirp.userserver.mapper.RelationMapper;
import com.zyq.chirp.userserver.model.enumeration.RelationType;
import com.zyq.chirp.userserver.model.pojo.Relation;
import com.zyq.chirp.userserver.service.RelationService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class RelationServiceImpl implements RelationService {
    @Resource
    RelationMapper relationMapper;
    @Resource
    RelationConvertor relationConvertor;
    @Resource
    DefaultKafkaProducer<SiteMessageDto> producer;
    @Value("${mq.topic.site-message.follow}")
    String followTopic;
    Integer expire = 6;

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
    public List<Relation> getUserRelation(Collection<Long> userIds, Long targetUserId) {
        return relationMapper.selectList(new LambdaQueryWrapper<Relation>()
                .eq(Relation::getFromId, targetUserId)
                .in(Relation::getToId, userIds));
    }

    @Override
    public List<Long> getFollower(Long userId, Integer page, Integer pageSize) {
        Page<Relation> selectPage = new Page<>(page, pageSize);
        selectPage.setSearchCount(false);
        return relationMapper.selectPage(selectPage, new LambdaQueryWrapper<Relation>()
                        .select(Relation::getFromId)
                        .eq(Relation::getToId, userId))
                .getRecords()
                .stream()
                .map(Relation::getFromId)
                .toList();
    }

    @Override
    public Long getFollowerCount(Long userId) {
        return relationMapper.selectCount(new LambdaQueryWrapper<Relation>()
                .eq(Relation::getToId, userId));
    }


    @Override
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
        SiteMessageDto messageDto = SiteMessageDto.builder()
                .receiverId(toId)
                .event(EventType.FOLLOW.name())
                .entityType(EntityType.USER.name())
                .senderId(fromId)
                .build();
        producer.avoidRedundancySend(fromId + ":" + toId, followTopic, messageDto, Duration.ofHours(expire));
    }

    @Override
    public void unfollow(Long fromId, Long toId) {
        if (Objects.isNull(fromId) || Objects.isNull(toId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "信息不完善");
        }
        relationMapper.delete(new LambdaQueryWrapper<Relation>()
                .eq(Relation::getFromId, fromId)
                .eq(Relation::getToId, toId)
                .eq(Relation::getStatus, RelationType.UNFOLLOWED.getRelation()));

    }

    @Override
    public void block(Long fromId, Long toId) {
        if (Objects.isNull(fromId) || Objects.isNull(toId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "信息不完善");
        } else if (fromId.equals(toId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "不能拉黑自己");
        }
        Relation relation = new Relation(fromId, toId, new Timestamp(System.currentTimeMillis()), RelationType.BLOCK.getRelation());
        relationMapper.replace(relation);
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
