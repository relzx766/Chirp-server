package com.zyq.chirp.chirperserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.rholder.retry.RetryException;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceclient.enums.EntityType;
import com.zyq.chirp.adviceclient.enums.EventType;
import com.zyq.chirp.adviceclient.enums.NoticeType;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.aspect.ParseMentioned;
import com.zyq.chirp.chirperserver.aspect.Statistic;
import com.zyq.chirp.chirperserver.convertor.ChirperConvertor;
import com.zyq.chirp.chirperserver.domain.enums.*;
import com.zyq.chirp.chirperserver.domain.pojo.Chirper;
import com.zyq.chirp.chirperserver.domain.pojo.Like;
import com.zyq.chirp.chirperserver.mapper.ChirperMapper;
import com.zyq.chirp.chirperserver.service.ChirperService;
import com.zyq.chirp.chirperserver.service.LikeService;
import com.zyq.chirp.common.domain.enums.OrderEnum;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.mq.enums.DefaultOperation;
import com.zyq.chirp.common.mq.model.Action;
import com.zyq.chirp.common.util.PageUtil;
import com.zyq.chirp.common.util.RetryUtil;
import com.zyq.chirp.common.util.TextUtil;
import com.zyq.chirp.mediaclient.client.MediaClient;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import com.zyq.chirp.userclient.enums.RelationType;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
    KafkaTemplate<String, Object> kafkaTemplate;
    @Resource
    RedisTemplate<String, Object> redisTemplate;
    @Value("${default-config.page-size}")
    Integer pageSize;
    @Value("${mq.topic.site-message.forward}")
    String FORWARD_MSG_TOPIC;
    @Value("${mq.topic.site-message.reply}")

    String REPLY_MSG_TOPIC;
    @Value("${mq.topic.site-message.quote}")

    String QUOTE_MSG_TOPIC;
    @Value("${mq.topic.chirper.forward.record}")
    String FORWARD_RECORD_TOPIC;
    @Value("${mq.topic.chirper.forward.count}")
    String FORWARD_INCREMENT_COUNT_TOPIC;
    @Value("${mq.topic.chirper.reply.record}")
    String REPLY_RECORD_TOPIC;
    @Value("${mq.topic.chirper.reply.count}")
    String REPLY_INCREMENT_COUNT_TOPIC;
    @Value("${mq.topic.chirper.quote.count}")
    String QUOTE_INCREMENT_COUNT_TOPIC;
    @Value("${mq.topic.chirper.quote.record}")
    String QUOTE_RECORD_TOPIC;
    Integer expire = 6;

    @Override
    @ParseMentioned
    public ChirperDto save(ChirperDto chirperDto) {
        chirperDto = this.getWithPrecondition(chirperDto);
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
    @Statistic(id = "#chirperDto.inReplyToChirperId", key = CacheKey.VIEW_COUNT_BOUND_KEY)
    @ParseMentioned
    public ChirperDto reply(ChirperDto chirperDto) {
        Chirper target = chirperMapper.selectOne(new LambdaQueryWrapper<Chirper>()
                .select(Chirper::getId, Chirper::getAuthorId, Chirper::getReplyRange)
                .eq(Chirper::getId, chirperDto.getInReplyToChirperId())
                .eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus()));
        ChirperDto targetDto = chirperConvertor.pojoToDto(target);
        if (targetDto == null) {
            throw new ChirpException(Code.ERR_BUSINESS, "回复失败，推文不存在或已被删除");
        }
        Map<Long, Boolean> replyableMap = this.getReplyable(List.of(targetDto), chirperDto.getAuthorId());
        if (!replyableMap.get(targetDto.getId())) {
            throw new ChirpException(Code.ERR_BUSINESS, STR."回复失败，该推文仅允许作者\{ReplyRangeEnums.getHint(chirperDto.getReplyRange())}的回复");
        }
        chirperDto = this.getWithPrecondition(chirperDto);
        Chirper chirper = chirperConvertor.dtoToPojo(chirperDto);
        chirper.setId(IdWorker.getId());
        chirper.setStatus(ChirperStatus.ACTIVE.getStatus());
        chirper.setType(ChirperType.REPLY.toString());
        chirper.setCreateTime(new Timestamp(System.currentTimeMillis()));
        boolean isInsert = chirperMapper.addReply(chirper) > 0;
        if (!isInsert) {
            throw new ChirpException(Code.ERR_BUSINESS, "回复失败");
        }
        //评论数量+1
        Action<Long, Long> action = new Action<>(ActionTypeEnums.REPLY.getAction(),
                DefaultOperation.INCREMENT.getOperation(),
                chirper.getAuthorId(),
                chirper.getInReplyToChirperId(),
                System.currentTimeMillis());
        kafkaTemplate.send(REPLY_INCREMENT_COUNT_TOPIC, action);
        //通知推送
        NotificationDto message = NotificationDto.builder()
                .sonEntity(chirperDto.getInReplyToChirperId().toString())
                .entity(chirper.getId().toString())
                .event(EventType.REPLY.name())
                .entityType(EntityType.CHIRPER.name())
                .noticeType(NoticeType.USER.name())
                .senderId(chirper.getAuthorId())
                .build();
        kafkaTemplate.send(REPLY_MSG_TOPIC, message);
        return chirperConvertor.pojoToDto(chirper);
    }

    @Override
    public void modifyReplyCount(List<Action<Long, Long>> actions) {
        Map<Long, List<Action<Long, Long>>> collect = actions.stream()
                .collect(Collectors.groupingBy(Action::getTarget));
        collect.forEach((chirperId, actionList) -> {
            int count = Action.getIncCount(actionList);
            try {
                RetryUtil.doDBRetry(() -> chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                        .eq(Chirper::getId, chirperId)
                        .setSql(STR."reply_count=reply_count+\{count}")));
            } catch (Exception e) {
                if (e instanceof ExecutionException) {
                    log.error("修改评论数时发生无法成功的错误，推文id:{}", chirperId, e);
                } else {
                    log.error("修改评论数失败,推文id:{}，数量:{},错误:", chirperId, count, e);
                    for (Action<Long, Long> action : actionList) {
                        log.warn("尝试重发:{}", action);
                        kafkaTemplate.send(REPLY_INCREMENT_COUNT_TOPIC, action);
                    }
                }

            }

        });
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
        try {
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
                var ref = new Object() {
                    Map<Long, Long> likeMap = new HashMap<>();
                    Map<Long, Map.Entry<Long, String>> reference = new HashMap<>();
                    Map<Long, Boolean> replyableMap = new HashMap<>();
                };

                int threadSize = 3;
                CountDownLatch latch = new CountDownLatch(threadSize);
                Thread.ofVirtual().start(() -> {
                    ref.likeMap = likeService.getLikeInfo(ids, userId)
                            .stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
                    latch.countDown();
                });
                Thread.ofVirtual().start(() -> {
                    ref.reference = chirperMapper.selectList(new LambdaQueryWrapper<Chirper>()
                                    .select(Chirper::getId, Chirper::getReferencedChirperId, Chirper::getType)
                                    .eq(Chirper::getAuthorId, userId)
                                    .eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                                    .in(Chirper::getReferencedChirperId, ids))
                            .stream()
                            .collect(Collectors.toMap(Chirper::getId, chirper ->
                                    Map.entry(chirper.getReferencedChirperId(), chirper.getType())));
                    latch.countDown();
                });
                Thread.ofVirtual().start(() -> {
                    ref.replyableMap = this.getReplyable(chirperDtos, userId);
                    latch.countDown();
                });
                latch.await();
                chirperDtos.forEach(chirperDto -> {
                    Long id = chirperDto.getId();
                    boolean isLike = ref.likeMap.get(id) != null;
                    boolean isForward = ref.reference.containsValue(Map.entry(id, ChirperType.FORWARD.name()));
                    boolean isQuote = ref.reference.containsValue(Map.entry(id, ChirperType.QUOTE.name()));
                    chirperDto.setIsLike(isLike);
                    chirperDto.setIsForward(isForward);
                    chirperDto.setIsQuote(isQuote);
                    chirperDto.setReplyable(ref.replyableMap.get(id));
                });
            }
            return chirperDtos;
        } catch (InterruptedException e) {
            throw new ChirpException(Code.ERR_SYSTEM, "获取推文信息失败");
        }
    }


    @Override
    @Statistic(id = "#chirperId",
            key = {CacheKey.VIEW_COUNT_BOUND_KEY})
    //防止被重复调用
    @Cacheable(cacheNames = "chirper:forward", key = "#chirperId+':'+#userId")
    public void forward(Long chirperId, Long userId) {
        Chirper chirper = Chirper.builder()
                .id(IdWorker.getId())
                .authorId(userId)
                .referencedChirperId(chirperId)
                .type(ChirperType.FORWARD.name())
                .createTime(new Timestamp(System.currentTimeMillis()))
                .status(ChirperStatus.ACTIVE.getStatus())
                .build();
        //是否更新都会返回1，该方法返回值恒>=1
        chirperMapper.addForward(chirper);
        Thread.ofVirtual().start(() -> {
            Action<Long, Long> action = new Action<>(
                    ActionTypeEnums.FORWARD.getAction(),
                    DefaultOperation.INCREMENT.getOperation(),
                    userId,
                    chirperId,
                    System.currentTimeMillis()
            );
            kafkaTemplate.send(FORWARD_INCREMENT_COUNT_TOPIC, action);
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(STR."\{EventType.FORWARD.name()}:\{userId}:\{chirperId}", 1, Duration.ofHours(expire));
            if (Boolean.TRUE.equals(absent)) {
                NotificationDto messageDto = NotificationDto.builder()
                        .sonEntity(String.valueOf(chirperId))
                        .entityType(EntityType.CHIRPER.name())
                        .event(EventType.FORWARD.name())
                        .senderId(userId)
                        .build();
                kafkaTemplate.send(FORWARD_MSG_TOPIC, messageDto);
            }
        });
    }

/*    @Override
    public void saveForward(List<Action<Long, Long>> actions) {
        //每个目标只取最后一个记录
        Map<Long, Optional<Action<Long, Long>>> collect = actions.stream().collect(Collectors.groupingBy(Action::getTarget,
                Collectors.maxBy(Comparator.comparingLong(Action::getActionTime))));
        List<Chirper> chirpers = new ArrayList<>();
        collect.forEach((chirperId, actionOp) -> {
            actionOp.ifPresent(action -> {
                Chirper chirper = Chirper.builder()
                        .id(IdWorker.getId())
                        .authorId(action.getOperator())
                        .referencedChirperId(action.getTarget())
                        .createTime(new Timestamp(action.getActionTime()))
                        .status(ChirperStatus.ACTIVE.getStatus())
                        .build();
                chirpers.add(chirper);
            });
        });
        try {
            RetryUtil.doDBRetry(() -> chirperMapper.addForwardBatch(chirpers));
            Thread.ofVirtual().start(() -> {
                chirpers.forEach(chirper -> {
                    Long chirperId = chirper.getReferencedChirperId();
                    Long userId = chirper.getAuthorId();
                    Action<Long, Long> action = new Action<>(
                            ActionTypeEnums.FORWARD.getAction(),
                            DefaultOperation.INCREMENT.getOperation(),
                            userId,
                            chirperId,
                            System.currentTimeMillis()
                    );
                    kafkaTemplate.send(FORWARD_INCREMENT_COUNT_TOPIC, action);

                });


            });
        } catch (ExecutionException e) {
            log.error("插入转发信息时发生无法成功的错误,推文列表:{},错误:", chirpers, e);
        } catch (Exception e) {
            log.error("插入转发失败,推文列表:{},错误:", chirpers, e);
            actions.forEach(action -> {
                log.warn("尝试重发:{}", action);
                kafkaTemplate.send(FORWARD_RECORD_TOPIC, action);
            });
        }

    }*/

    @Override
    @CacheEvict(cacheNames = "chirper:forward", key = "#chirperId+':'+#userId")
    public boolean cancelForward(Long chirperId, Long userId) {
        long currentTimeMillis = System.currentTimeMillis();
        boolean isUpdate = chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                .eq(Chirper::getReferencedChirperId, chirperId)
                .eq(Chirper::getAuthorId, userId)
                .eq(Chirper::getType, ChirperType.FORWARD.name())
                .eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                .ge(Chirper::getCreateTime, currentTimeMillis)
                .set(Chirper::getStatus, ChirperStatus.DELETE.getStatus())
                .set(Chirper::getCreateTime, new Timestamp(currentTimeMillis))) > 0;
        if (isUpdate) {
            Action<Long, Long> action = new Action<>(
                    ActionTypeEnums.FORWARD.getAction(),
                    DefaultOperation.DECREMENT.getOperation(),
                    userId,
                    chirperId,
                    currentTimeMillis
            );
            kafkaTemplate.send(FORWARD_INCREMENT_COUNT_TOPIC, action);
        }
        return isUpdate;
    }

   /* @Override
    public void saveForwardCancel(List<Action<Long, Long>> actions) {
        //每个目标只取最后一个记录
        Map<Long, Optional<Action<Long, Long>>> collect = actions.stream().collect(Collectors.groupingBy(Action::getTarget,
                Collectors.maxBy(Comparator.comparingLong(Action::getActionTime))));
        collect.forEach((chirperId, actionOp) -> {
            actionOp.ifPresent(action -> {
                try {
                    //kafka不保证不同分区消息的顺序，保证有影响的永远是最近一条
                    RetryUtil.doDBRetry(() -> chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                            .eq(Chirper::getReferencedChirperId, action.getTarget())
                            .eq(Chirper::getAuthorId, action.getOperator())
                            .ge(Chirper::getCreateTime, action.getActionTime())
                            .set(Chirper::getStatus, ChirperStatus.DELETE.getStatus())
                            .set(Chirper::getCreateTime, new Timestamp(action.getActionTime()))));
                    action.setOperation(DefaultOperation.DECREMENT.getOperation());
                    action.setActionTime(System.currentTimeMillis());
                    kafkaTemplate.send(FORWARD_INCREMENT_COUNT_TOPIC, action);
                } catch (ExecutionException e) {
                    log.error("持久化取消转发发生无法成功的错误,被转发推文:{},转发者:{},错误:", action.getTarget(), action.getOperator(), e);
                } catch (Exception e) {
                    log.error("持久化取消转发失败,被转发推文:{},转发者:{},错误:", action.getTarget(), action.getOperator(), e);
                    log.warn("尝试重发:{}", action);
                    kafkaTemplate.send(FORWARD_RECORD_TOPIC, action);
                }
            });
        });
    }*/

    @Override
    public void modifyForwardCount(List<Action<Long, Long>> actions) {
        Map<Long, List<Action<Long, Long>>> collect = actions.stream()
                .collect(Collectors.groupingBy(Action::getTarget));
        collect.forEach((chirperId, actionList) -> {
            int count = Action.getIncCount(actionList);
            try {
                RetryUtil.doDBRetry(() -> chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                        .eq(Chirper::getId, chirperId)
                        .setSql(STR."forward_count=forward_count+\{count}")));
            } catch (ExecutionException e) {
                log.error("修改转发数时发生无法成功的错误，推文id:{}", chirperId, e);
            } catch (Exception e) {
                log.error("修改转发数失败,推文id:{}，数量:{},错误:", chirperId, count, e);
                for (Action<Long, Long> action : actionList) {
                    log.warn("尝试重发:{}", action);
                    kafkaTemplate.send(FORWARD_INCREMENT_COUNT_TOPIC, action);
                }
            }
        });
    }


    @Override
    @Transactional
    @Statistic(id = "#chirperDto.referencedChirperId", key = CacheKey.VIEW_COUNT_BOUND_KEY)
    @ParseMentioned
    public ChirperDto quote(ChirperDto chirperDto) {
        chirperDto = this.getWithPrecondition(chirperDto);
        Chirper chirper = chirperConvertor.dtoToPojo(chirperDto);
        chirper.setId(IdWorker.getId());
        chirper.setConversationId(chirper.getId());
        chirper.setType(ChirperType.QUOTE.toString());
        chirper.setStatus(ChirperStatus.ACTIVE.getStatus());
        chirper.setCreateTime(new Timestamp(System.currentTimeMillis()));
        boolean isInsert = chirperMapper.addQuote(chirper) > 0;
        if (!isInsert) {
            throw new ChirpException(Code.ERR_BUSINESS, "发布失败");
        }
        Action<Long, Long> action = new Action<>(
                ActionTypeEnums.QUOTE.getAction(),
                DefaultOperation.INCREMENT.getOperation(),
                chirper.getAuthorId(),
                chirper.getReferencedChirperId(),
                System.currentTimeMillis()
        );
        kafkaTemplate.send(QUOTE_INCREMENT_COUNT_TOPIC, action);
        NotificationDto messageDto = NotificationDto.builder()
                .sonEntity(chirperDto.getReferencedChirperId().toString())
                .entity(chirper.getId().toString())
                .noticeType(NoticeType.USER.name())
                .event(EventType.QUOTE.name())
                .entityType(EntityType.CHIRPER.name())
                .senderId(chirper.getAuthorId()).build();
        kafkaTemplate.send(QUOTE_MSG_TOPIC, messageDto);
        return chirperConvertor.pojoToDto(chirper);
    }

    @Override
    public void modifyQuoteCount(List<Action<Long, Long>> actions) {
        Map<Long, List<Action<Long, Long>>> collect = actions.stream().collect(Collectors.groupingBy(Action::getTarget));
        collect.forEach((chirperId, actionList) -> {
            int count = Action.getIncCount(actionList);
            try {
                RetryUtil.doDBRetry(() -> chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                        .eq(Chirper::getId, chirperId)
                        .setSql(STR."quote_count=quote_count+\{count}")));
            } catch (ExecutionException e) {
                log.error("修改引用数时发生无法成功的错误，推文id:{}", chirperId, e);
            } catch (RetryException e) {
                log.error("修改引用数失败,推文id:{}，数量:{},错误:", chirperId, count, e);
                for (Action<Long, Long> action : actionList) {
                    log.warn("尝试重发:{}", action);
                    kafkaTemplate.send(QUOTE_INCREMENT_COUNT_TOPIC, action);
                }
            }
        });
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
    public List<ChirperDto> getById(List<Long> chirperIds) {
        if (chirperIds == null || chirperIds.isEmpty()) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供id");
        }
        Map<Long, ChirperDto> chirperDtoMap = chirperMapper.selectList(new LambdaQueryWrapper<Chirper>()
                        .in(Chirper::getId, chirperIds)
                        .eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus()))
                .stream()
                .map(chirper -> chirperConvertor.pojoToDto(chirper))
                //按照给出的推文id顺序排序
                .sorted(Comparator.comparing(chirperDto -> chirperIds.indexOf(chirperDto.getId())))
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
    @Cacheable(value = "chirper:page#4", key = "#page+':'+#chirperId+':'+#userIds+':'+#type+':'+#isMedia+':'+#order")
    public List<ChirperDto> getPage(Integer page, Long chirperId, Collection<Long> userIds, ChirperType type, Boolean isMedia, String order) {
        Page<Chirper> pageSelector = new Page<>(page, pageSize, false);
        LambdaQueryWrapper<Chirper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus());
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
        OrderEnum orderEnum = OrderEnum.findAndDefault(order);
        switch (orderEnum) {
            case ASC -> wrapper.orderByAsc(Chirper::getCreateTime);
            case DESC -> wrapper.orderByDesc(Chirper::getCreateTime);
            case HOT -> wrapper.orderByDesc(Chirper::getViewCount);
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
        return Optional.ofNullable(keyword)
                .map(matchWord -> {
                    Page<Chirper> searchPage = new Page<>(page, pageSize);
                    searchPage.setSearchCount(false);
                    LambdaQueryWrapper<Chirper> wrapper = new LambdaQueryWrapper<Chirper>()
                            .eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                            .orderByDesc(Chirper::getCreateTime)
                            .like(Chirper::getText, matchWord);
                    if (isMedia) {
                        wrapper.isNotNull(Chirper::getMediaKeys);
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
        System.out.println(likeRecord);
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

    @Override
    public List<ChirperDto> getByFollowerId(Long userId, Integer size) {
        List<Long> authorIds = userClient.getFollowingIds(userId).getBody();
        if (authorIds != null && !authorIds.isEmpty()) {
            Collections.shuffle(authorIds);
            int startIndex = 0;
            int endIndex = Math.min(authorIds.size(), 100);
            authorIds = authorIds.subList(startIndex, endIndex);
            int page = 1;
            Page<Chirper> selectPage = new Page<>(page, size);
            selectPage.setSearchCount(false);
            return chirperMapper.selectPage(selectPage, new LambdaQueryWrapper<Chirper>()
                            .select(Chirper::getId, Chirper::getAuthorId, Chirper::getCreateTime)
                            .orderByDesc(Chirper::getCreateTime)
                            .eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                            .in(Chirper::getAuthorId, authorIds))
                    .getRecords()
                    .stream()
                    .map(chirper -> chirperConvertor.pojoToDto(chirper))
                    .toList();
        }
        return List.of();
    }

    @Override
    public Map<Long, Boolean> getReplyable(List<ChirperDto> chirperDtos, Long userId) {
        Set<Long> authors = chirperDtos.stream()
                .map(ChirperDto::getAuthorId)
                .collect(Collectors.toSet());
        authors.add(userId);
        ResponseEntity<List<UserDto>> response = userClient.getUsernameAndRelation(authors, userId);
        Map<Long, UserDto> userDtoMap = new HashMap<>();
        if (response.getStatusCode().is2xxSuccessful()) {
            List<UserDto> userDtoList = response.getBody();
            if (userDtoList != null && !userDtoList.isEmpty()) {
                userDtoMap = userDtoList.stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
            }
        }
        Map<Long, Boolean> replyable = new HashMap<>();
        UserDto targetUser = userDtoMap.get(userId);
        for (ChirperDto chirperDto : chirperDtos) {
            Long chirperId = chirperDto.getId();
            if (chirperDto.getAuthorId().equals(userId)) {
                replyable.put(chirperId, true);
                continue;
            }
            Integer replyRange = chirperDto.getReplyRange();
            replyRange = replyRange != null ? replyRange : ReplyRangeEnums.EVERYONE.getCode();
            if (ReplyRangeEnums.EVERYONE.getCode() == replyRange) {
                replyable.put(chirperId, true);
            } else {
                boolean isMentioned = false;
                if (targetUser != null) {
                    isMentioned = TextUtil.getIsMentioned(chirperDto.getText(), targetUser.getUsername());
                }
                if (isMentioned) {
                    replyable.put(chirperId, true);
                } else {
                    if (ReplyRangeEnums.FOLLOWING.getCode() == replyRange) {
                        UserDto author = userDtoMap.get(chirperDto.getAuthorId());
                        boolean able = author != null && RelationType.FOLLOWING.getRelation() == author.getRelation();
                        replyable.put(chirperId, able);
                    } else if (ReplyRangeEnums.MENTION.getCode() == replyRange) {
                        replyable.put(chirperId, false);
                    }
                }
            }
        }
        return replyable;
    }

    @Override
    public ChirperDto getWithPrecondition(ChirperDto chirperDto) {
        if (chirperDto.isEmpty()) {
            throw new ChirpException(Code.ERR_BUSINESS, "推文内容不能为空");
        }
        ReplyRangeEnums rangeEnums = ReplyRangeEnums.findByCodeWithDefault(chirperDto.getReplyRange());
        chirperDto.setReplyRange(rangeEnums.getCode());
        return chirperDto;
    }

}
