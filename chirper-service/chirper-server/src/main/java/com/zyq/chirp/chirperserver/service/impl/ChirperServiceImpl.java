package com.zyq.chirp.chirperserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceclient.enums.EntityType;
import com.zyq.chirp.adviceclient.enums.EventType;
import com.zyq.chirp.adviceclient.enums.NoticeType;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.aspect.ParseMentioned;
import com.zyq.chirp.chirperserver.aspect.Statistic;
import com.zyq.chirp.chirperserver.convertor.ChirperConvertor;
import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.chirperserver.domain.enums.ChirperStatus;
import com.zyq.chirp.chirperserver.domain.enums.ChirperType;
import com.zyq.chirp.chirperserver.domain.pojo.Chirper;
import com.zyq.chirp.chirperserver.domain.pojo.Like;
import com.zyq.chirp.chirperserver.mapper.ChirperMapper;
import com.zyq.chirp.chirperserver.service.ChirperService;
import com.zyq.chirp.chirperserver.service.LikeService;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.util.PageUtil;
import com.zyq.chirp.mediaclient.client.MediaClient;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChirperServiceImpl implements ChirperService {
    @Resource
    ChirperMapper chirperMapper;
    @Resource
    LikeService likeService;
    @Resource
    ChirperConvertor chirperConvertor;
    @Resource
    MediaClient mediaClient;
    @Resource
    UserClient userClient;
    @Resource
    ObjectMapper objectMapper;
    @Resource
    KafkaTemplate<String, Object> kafkaTemplate;
    @Resource
    RedisTemplate<String, Object> redisTemplate;
    @Value("${default-config.page-size}")
    Integer pageSize;
    @Value("${mq.topic.site-message.forward}")
    String forward;
    @Value("${mq.topic.site-message.reply}")

    String reply;
    @Value("${mq.topic.site-message.quote}")

    String quote;
    Integer expire = 6;

    @Override
    @ParseMentioned
    public ChirperDto save(ChirperDto chirperDto) {
        if (chirperDto.isEmpty()) {
            throw new ChirpException(Code.ERR_BUSINESS, "推文内容不能为空");
        }
        Chirper chirper = chirperConvertor.dtoToPojo(chirperDto);
        chirper.setId(IdWorker.getId());
        chirper.setConversationId(chirper.getId());
        chirper.setStatus(ChirperStatus.ACTIVE.getStatus());
        chirper.setType(ChirperType.ORIGINAL.toString());
        chirper.setCreateTime(new Timestamp(System.currentTimeMillis()));
        chirperMapper.insert(chirper);
        return chirperConvertor.pojoToDto(chirper);
    }

    @Override
    @Transactional
    @Statistic(id = "#chirperDto.inReplyToChirperId", key = CacheKey.VIEW_COUNT_BOUND_KEY)
    @ParseMentioned
    public ChirperDto reply(ChirperDto chirperDto) {
        if (chirperDto.isEmpty()) {
            throw new ChirpException(Code.ERR_BUSINESS, "推文内容不能为空");
        }
        Chirper chirper = chirperConvertor.dtoToPojo(chirperDto);
        chirper.setId(IdWorker.getId());
        chirper.setStatus(ChirperStatus.ACTIVE.getStatus());
        chirper.setType(ChirperType.REPLY.toString());
        chirper.setCreateTime(new Timestamp(System.currentTimeMillis()));
        boolean isInsert = chirperMapper.addReply(chirper) > 0;
        boolean isSet = chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                .setSql("reply_count=reply_count+1")
                .eq(Chirper::getId, chirper.getInReplyToChirperId())) > 0;
        if (!isInsert || !isSet) {
            throw new ChirpException(Code.ERR_BUSINESS, "回复失败");
        }
        NotificationDto message = NotificationDto.builder()
                .sonEntity(chirperDto.getInReplyToChirperId().toString())
                .entity(chirper.getId().toString())
                .event(EventType.REPLY.name())
                .entityType(EntityType.CHIRPER.name())
                .noticeType(NoticeType.USER.name())
                .senderId(chirper.getAuthorId())
                .build();
        kafkaTemplate.send(reply, message);
        return chirperConvertor.pojoToDto(chirper);
    }

    @Override
    public Map<Long, ChirperDto> fetchReference(Collection<Long> ids) {
        return chirperMapper.selectList(new LambdaQueryWrapper<Chirper>().in(Chirper::getId, ids))
                .stream()
                .map(chirper -> chirperConvertor.pojoToDto(chirper))
                .collect(Collectors.toMap(ChirperDto::getId, Function.identity()));
    }

    @Override
    public List<ChirperDto> getInteractionInfo(List<ChirperDto> chirperDtos, Long userId) {
        List<Long> ids = chirperDtos.stream().map(ChirperDto::getId).toList();
        if (!ids.isEmpty()) {
            //获取被引用推文的互动信息---
            Map<Integer, ChirperDto> refers = new HashMap<>();
            for (int i = 0; i < chirperDtos.size(); i++) {
                if (ChirperType.FORWARD.name().equals(chirperDtos.get(i).getType()) &&
                        chirperDtos.get(i).getReferenced() != null) {
                    refers.put(i, chirperDtos.get(i).getReferenced());
                }
            }
            if (!refers.isEmpty()) {
                this.getInteractionInfo(new ArrayList<>(refers.values()), userId).forEach(chirperDto -> {
                    refers.forEach((k, v) -> {
                        if (chirperDto.getId().equals(v.getId())) {
                            chirperDtos.get(k).setReferenced(chirperDto);
                        }
                    });
                });
            }
            //---
            Map<Long, Long> likeMap = likeService.getLikeInfo(ids, userId)
                    .stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
            Map<Long, Map.Entry<Long, String>> reference = chirperMapper.selectList(new LambdaQueryWrapper<Chirper>()
                            .select(Chirper::getId, Chirper::getReferencedChirperId, Chirper::getType)
                            .eq(Chirper::getAuthorId, userId)
                            .eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                            .in(Chirper::getReferencedChirperId, ids))
                    .stream()
                    .collect(Collectors.toMap(Chirper::getId, chirper ->
                            Map.entry(chirper.getReferencedChirperId(), chirper.getType())));
            chirperDtos.forEach(chirperDto -> {
                Long id = chirperDto.getId();
                boolean isLike = likeMap.get(id) != null;
                boolean isForward = reference.containsValue(Map.entry(id, ChirperType.FORWARD.name()));
                boolean isQuote = reference.containsValue(Map.entry(id, ChirperType.QUOTE.name()));
                chirperDto.setIsLike(isLike);
                chirperDto.setIsForward(isForward);
                chirperDto.setIsQuote(isQuote);
            });
        }
        return chirperDtos;
    }


    @Override
    @Transactional
    @Statistic(id = "#chirperId",
            key = {CacheKey.VIEW_COUNT_BOUND_KEY, CacheKey.FORWARD_COUNT_BOUND_KEY})
    @Cacheable(cacheNames = "chirper:forward#1", key = "#chirperId+':'+#userId")
    public void forward(Long chirperId, Long userId) {
        Chirper record = chirperMapper.selectOne(new LambdaQueryWrapper<Chirper>().eq(Chirper::getReferencedChirperId, chirperId)
                .eq(Chirper::getAuthorId, userId)
                .eq(Chirper::getType, ChirperType.FORWARD));
        if (record == null) {
            Chirper chirper = new Chirper();
            chirper.setReferencedChirperId(chirperId);
            chirper.setAuthorId(userId);
            chirper.setType(ChirperType.FORWARD.name());
            chirper.setStatus(ChirperStatus.ACTIVE.getStatus());
            chirper.setCreateTime(new Timestamp(System.currentTimeMillis()));
            chirperMapper.insert(chirper);
        } else {
            chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                    .set(Chirper::getCreateTime, new Timestamp(System.currentTimeMillis()))
                    .set(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                    .eq(Chirper::getReferencedChirperId, chirperId)
                    .eq(Chirper::getAuthorId, userId)
                    .eq(Chirper::getType, ChirperType.FORWARD.name()));
        }
        Thread.ofVirtual().start(() -> {
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(STR. "\{ EventType.FORWARD.name() }:\{ userId }:\{ chirperId }" , 1, Duration.ofHours(expire));
            if (Boolean.TRUE.equals(absent)) {
                NotificationDto messageDto = NotificationDto.builder()
                        .sonEntity(String.valueOf(chirperId))
                        .entityType(EntityType.CHIRPER.name())
                        .event(EventType.FORWARD.name())
                        .senderId(userId)
                        .build();
                kafkaTemplate.send(forward, messageDto);
            }

        });
    }

    @Override
    @Statistic(id = "#chirperId", delta = -1, key = CacheKey.FORWARD_COUNT_BOUND_KEY)
    @CacheEvict(cacheNames = "chirper:forward", key = "#chirperId+':'+#userId")
    public void cancelForward(Long chirperId, Long userId) {
        boolean flag = chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                .set(Chirper::getStatus, ChirperStatus.DELETE.getStatus())
                .eq(Chirper::getReferencedChirperId, chirperId)
                .eq(Chirper::getAuthorId, userId)
                .eq(Chirper::getType, ChirperType.FORWARD.name())) > 0;
        if (!flag) {
            throw new ChirpException(Code.ERR_BUSINESS, "取消转发失败");
        }
    }


    @Override
    @Transactional
    @Statistic(id = "#chirperDto.referencedChirperId", key = CacheKey.VIEW_COUNT_BOUND_KEY)
    @ParseMentioned
    public ChirperDto quote(ChirperDto chirperDto) {
        if (chirperDto.isEmpty()) {
            throw new ChirpException(Code.ERR_BUSINESS, "推文内容不能为空");
        }
        Chirper refer = chirperMapper.selectOne(new LambdaQueryWrapper<Chirper>().select(Chirper::getAuthorId).eq(Chirper::getId, chirperDto.getReferencedChirperId()));
        if (refer != null) {
            Chirper chirper = chirperConvertor.dtoToPojo(chirperDto);
            chirper.setId(IdWorker.getId());
            chirper.setConversationId(chirper.getId());
            chirper.setType(ChirperType.QUOTE.toString());
            chirper.setStatus(ChirperStatus.ACTIVE.getStatus());
            chirper.setCreateTime(new Timestamp(System.currentTimeMillis()));
            boolean isInsert = chirperMapper.insert(chirper) > 0;
            chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                    .setSql("quote_count=quote_count+1")
                    .eq(Chirper::getId, chirper.getReferencedChirperId()));
            if (!isInsert) {
                throw new ChirpException(Code.ERR_BUSINESS, "发布失败");
            }

            NotificationDto messageDto = NotificationDto.builder()
                    .sonEntity(chirperDto.getReferencedChirperId().toString())
                    .entity(chirper.getId().toString())
                    .noticeType(NoticeType.USER.name())
                    .event(EventType.QUOTE.name())
                    .entityType(EntityType.CHIRPER.name())
                    .senderId(chirper.getAuthorId()).build();
            kafkaTemplate.send(quote, messageDto);
            return chirperConvertor.pojoToDto(chirper);
        } else {
            throw new ChirpException(Code.ERR_BUSINESS, "引用对象不存在");
        }
    }

    @Override
    public void delete(@NotNull Long chirperId, @NotNull Long currentUserId) {
        boolean flag = chirperMapper.update(null,
                new LambdaUpdateWrapper<Chirper>()
                        .eq(Chirper::getId, chirperId)
                        .eq(Chirper::getAuthorId, chirperId)
                        .set(Chirper::getStatus, ChirperStatus.DELETE.getStatus())) > 0;
        if (!flag) {
            throw new ChirpException(Code.ERR_BUSINESS, "删除失败");
        }
    }


    @Override
    @ParseMentioned
    public List<ChirperDto> getById(Collection<Long> chirperIds) {
        if (chirperIds == null || chirperIds.isEmpty()) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供id");
        }
        Map<Long, ChirperDto> chirperDtoMap = chirperMapper.selectList(new LambdaQueryWrapper<Chirper>()
                        .in(Chirper::getId, chirperIds)
                        .eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus()))
                .stream()
                .map(chirper -> chirperConvertor.pojoToDto(chirper))
                .collect(Collectors.toMap(ChirperDto::getId, Function.identity(), (k1, k2) -> k1, LinkedHashMap::new));
        Map<Long, Long> referMap = chirperDtoMap.values().stream()
                .filter(chirperDto -> ChirperType.QUOTE.name().equals(chirperDto.getType()))
                .collect(Collectors.toMap(ChirperDto::getId, ChirperDto::getReferencedChirperId));
        if (!referMap.isEmpty()) {
            Map<Long, ChirperDto> references = this.fetchReference(referMap.values());
            referMap.forEach((k, v) -> {
                ChirperDto refer = chirperDtoMap.get(k);
                refer.setReferenced(references.get(v));
                chirperDtoMap.put(k, refer);
            });
        }
        return this.combineWithMedia(chirperDtoMap.values());
    }

    @Override
    @Cacheable(value = "chirper:page#4", key = "#page+':'+#chirperId+':'+#userIds+':'+#type+':'+#isMedia")
    public List<ChirperDto> getPage(Integer page, Long chirperId, Collection<Long> userIds, ChirperType type, Boolean isMedia) {
        Page<Chirper> pageSelector = new Page<>(page, pageSize, false);
        LambdaQueryWrapper<Chirper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus());
        wrapper.orderByDesc(Chirper::getCreateTime);
        if (chirperId != null) {
            wrapper.eq(Chirper::getInReplyToChirperId, chirperId);
        }
        if (userIds != null && !userIds.isEmpty()) {
            wrapper.in(Chirper::getAuthorId, userIds);
        }
        if (type != null) {
            wrapper.eq(Chirper::getType, type.name());
        }
        if (isMedia != null && isMedia) {
            wrapper.isNotNull(Chirper::getMediaKeys);
        }
        //转换为map类型，为下面获取被引用推文准备
        Map<Long, ChirperDto> chirperDtoMap = chirperMapper.selectPage(pageSelector, wrapper).getRecords()
                .stream()
                .map(chirper -> chirperConvertor.pojoToDto(chirper))
                .collect(Collectors.toMap(ChirperDto::getId, Function.identity(), (k1, k2) -> k1, LinkedHashMap::new));
        //引用推文
        Map<Long, Long> referMap = chirperDtoMap.values().stream().filter(chirperDto ->
                        ChirperType.FORWARD.name().equals(chirperDto.getType())
                                || ChirperType.QUOTE.name().equals(chirperDto.getType()))
                .collect(Collectors.toMap(ChirperDto::getId, ChirperDto::getReferencedChirperId));
        if (!referMap.isEmpty()) {
            //被引用推文
            Map<Long, ChirperDto> fetchReference = this.fetchReference(referMap.values());
            referMap.forEach((k, v) -> {
                ChirperDto refer = chirperDtoMap.get(k);
                refer.setReferenced(fetchReference.get(v));
                chirperDtoMap.put(k, refer);
            });
        }
        return this.combineWithMedia(chirperDtoMap.values());
    }

    /**
     * @return 关键词为空或null返回空列表
     */
    @Override
    @Cacheable(value = "chirper:search#2", key = "#keyword+':'+#page+':'+#isMedia")
    public List<ChirperDto> search(String keyword, Integer page, Boolean isMedia) {
        int offset = PageUtil.getOffset(page, pageSize);

        return Optional.ofNullable(keyword)
                .map(matchWord -> {
                    Page<Chirper> searchPage = new Page<>(page, pageSize);
                    searchPage.setSearchCount(false);
                    LambdaQueryWrapper<Chirper> wrapper = new LambdaQueryWrapper<Chirper>()
                            .eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                            .orderByDesc(Chirper::getCreateTime)
                            .like(Chirper::getText, keyword);
                    if (isMedia) {
                        wrapper.apply("JSON_LENGTH(media_keys)<>0");
                    }
                    Map<Long, ChirperDto> chirperDtoMap = chirperMapper.selectPage(searchPage, wrapper)
                            .getRecords()
                            .stream()
                            .map(chirper -> chirperConvertor.pojoToDto(chirper))
                            .collect(Collectors.toMap(ChirperDto::getId, Function.identity()));
                    //forward类型的推文没有内容，不会被查出来，只需过滤出quote类型的
                    Map<Long, Long> referMap = chirperDtoMap.values().stream().filter(chirperDto ->
                                    ChirperType.QUOTE.name().equals(chirperDto.getType()))
                            .collect(Collectors.toMap(ChirperDto::getId, ChirperDto::getReferencedChirperId));
                    if (!referMap.isEmpty()) {
                        Map<Long, ChirperDto> fetchReference = this.fetchReference(referMap.values());
                        referMap.forEach((k, v) -> {
                            ChirperDto refer = chirperDtoMap.get(k);
                            refer.setReferenced(fetchReference.get(v));
                            chirperDtoMap.put(k, refer);
                        });
                    }
                    return this.combineWithMedia(chirperDtoMap.values());
                })
                .orElse(List.of());
    }

    @Override
    public List<ChirperDto> getFollowing(Integer page, Long userId) {
        return null;
    }


    @Override
    @Cacheable(cacheNames = "chirper:like#2", key = "#userId+':'+#page")
    public List<ChirperDto> getLikeRecordByUserId(Long userId, Integer page) {
        List<Like> likeRecord = likeService.getLikeRecord(userId, page);
        if (!likeRecord.isEmpty()) {
            List<Long> chirperIds = likeRecord.stream().map(Like::getChirperId).toList();
            return this.getById(chirperIds);
        }
        return List.of();
    }

    @Override
    public void updateStatus(Long chirperId, ChirperStatus chirperStatus) {
        try {
            chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                    .set(Chirper::getStatus, chirperStatus.getStatus())
                    .eq(Chirper::getId, chirperId));
        } catch (Exception e) {
            updateStatus(chirperId, chirperStatus);
        }
    }

    @Override
    public int updateView(Long chirperId, Integer delta) {
        return chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                .setSql("view_count=view_count+" + delta)
                .eq(Chirper::getId, chirperId));
    }

    @Override
    public int updateForward(Long chirperId, Integer delta) {
        return chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                .setSql("forward_count=forward_count+" + delta)
                .eq(Chirper::getId, chirperId));
    }


    @Override
    public List<ChirperDto> getBasicInfo(Collection<Long> chirperIds) {
        return chirperMapper.selectList(new LambdaQueryWrapper<Chirper>()
                        .select(Chirper::getId, Chirper::getAuthorId, Chirper::getType)
                        .in(Chirper::getId, chirperIds))
                .stream()
                .map(chirper -> chirperConvertor.pojoToDto(chirper))
                .toList();
    }

    @Override
    public List<ChirperDto> combineWithMedia(Collection<ChirperDto> chirperDtos) {
        try {
            if (chirperDtos == null || chirperDtos.isEmpty()) {
                return List.of();
            }
            //提取出所有的媒体信息
            Map<Long, List<Integer>> map = chirperDtos.stream()
                    .map(chirperDto -> {
                        try {
                            //递归获取引用推文的媒体信息
                            if (chirperDto.getReferenced() != null) {
                                chirperDto.setReferenced(this.combineWithMedia(List.of(chirperDto.getReferenced())).get(0));
                            }
                            List<Integer> mediaKeys = chirperDto.getMediaKeys().stream().map(MediaDto::getId).toList();
                            return Map.entry(chirperDto.getId(), mediaKeys);
                        } catch (NullPointerException e) {
                            return Map.entry(chirperDto.getId(), List.<Integer>of());
                        }
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Long, List<MediaDto>> mediaMap = new HashMap<>();
            Map<Long, UserDto> userDtoMap = new HashMap<>();
            CountDownLatch latch = new CountDownLatch(2);
            Thread.ofVirtual().start(() -> {
                try {
                    List<Long> userIds = chirperDtos.stream().map(ChirperDto::getAuthorId).toList();
                    Map<Long, UserDto> userCollect = userClient.getBasicInfo(userIds).getBody().stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
                    userDtoMap.putAll(userCollect);
                } catch (Exception e) {
                    log.error("{}", e);
                } finally {
                    latch.countDown();
                }

            });
            Thread.ofVirtual().start(() -> {
                try {
                    Map<Long, List<MediaDto>> medias = mediaClient.getCombine(map).getBody();
                    mediaMap.putAll(medias);
                } catch (Exception e) {
                    log.error("{}", e);
                } finally {
                    latch.countDown();
                }
            });
            latch.await();
            chirperDtos.forEach(chirperDto -> {
                if (!mediaMap.isEmpty()) {
                    List<MediaDto> mediaDtos = mediaMap.get(chirperDto.getId());
                    mediaDtos = mediaDtos != null ? mediaDtos : List.of();
                    chirperDto.setMediaKeys(mediaDtos);
                    }
                if (!userDtoMap.isEmpty()) {
                        UserDto userDto = userDtoMap.get(chirperDto.getAuthorId());
                        chirperDto.setUsername(userDto.getUsername());
                        chirperDto.setNickname(userDto.getNickname());
                        chirperDto.setAvatar(userDto.getSmallAvatarUrl());
                    }
            });
        } catch (InterruptedException e) {
            throw new ChirpException(e);
        }
        return new ArrayList<>(chirperDtos);
    }

    @Override
    public Map<Object, Map<String, Object>> getTrend(Integer page, String type) {
        try {
            int offset = PageUtil.getOffset(page, pageSize);
            ZSetOperations<String, Object> opsForZSet = redisTemplate.opsForZSet();
            Set<ZSetOperations.TypedTuple<Object>> tend = opsForZSet.reverseRangeWithScores(
                    CacheKey.TEND_TAG_BOUND_KEY.getKey(), offset, (long) page * pageSize);
            Map<Object, Map<String, Object>> tendMap = new LinkedHashMap<>();
            if (tend != null && !tend.isEmpty()) {
                tend.forEach(tuple -> {
                    if (tuple.getValue() != null) {
                        Map<String, Object> trend = new HashMap<>();
                        trend.put("score", tuple.getScore());
                        Double score = opsForZSet.score(CacheKey.TEND_POST_BOUND_KEY.getKey(), tuple.getValue());
                        if (score != null) {
                            trend.put("post", score);
                        }
                        tendMap.put(tuple.getValue(), trend);
                    }

                });
            }
            return tendMap;
        } catch (NullPointerException e) {
            throw new ChirpException(Code.ERR_BUSINESS, "没有相关数据");
        }

    }

    @Override
    public Map<Long, List<Long>> getAllIdByAuthors(Collection<Long> userIds) {
        List<Chirper> chirpers = chirperMapper.selectList(new LambdaQueryWrapper<Chirper>()
                .select(Chirper::getId, Chirper::getAuthorId)
                .in(Chirper::getAuthorId, userIds));
        if (!chirpers.isEmpty()) {
            return chirpers.stream()
                    .collect(Collectors.groupingBy(
                            Chirper::getAuthorId, Collectors.mapping(Chirper::getId, Collectors.toList())));
        }
        return Map.of();
    }

    @Override
    public Long getAuthorIdByChirperId(Long chirperId) {
        Chirper chirper = chirperMapper.selectOne(new LambdaQueryWrapper<Chirper>()
                .select(Chirper::getAuthorId)
                .eq(Chirper::getId, chirperId));
        if (chirper != null) {
            return chirper.getAuthorId();
        } else {
            throw new ChirpException(Code.ERR_BUSINESS, "推文不存在");
        }
    }


}
