package com.zyq.chirp.chirperserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.rholder.retry.RetryException;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirpclient.dto.ChirperQueryDto;
import com.zyq.chirp.chirperserver.aspect.ParseMentioned;
import com.zyq.chirp.chirperserver.aspect.Statistic;
import com.zyq.chirp.chirperserver.convertor.ChirperConvertor;
import com.zyq.chirp.chirperserver.domain.enums.*;
import com.zyq.chirp.chirperserver.domain.pojo.Chirper;
import com.zyq.chirp.chirperserver.domain.pojo.Like;
import com.zyq.chirp.chirperserver.mapper.ChirperMapper;
import com.zyq.chirp.chirperserver.service.ChirperService;
import com.zyq.chirp.chirperserver.service.LikeService;
import com.zyq.chirp.common.domain.enums.ApproveEnum;
import com.zyq.chirp.common.domain.enums.OrderEnum;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.mq.enums.DefaultOperation;
import com.zyq.chirp.common.mq.model.Action;
import com.zyq.chirp.common.util.PageUtil;
import com.zyq.chirp.common.util.RetryUtil;
import com.zyq.chirp.common.util.StringUtil;
import com.zyq.chirp.common.util.TextUtil;
import com.zyq.chirp.communityclient.client.CommunityClient;
import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.mediaclient.client.MediaClient;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import com.zyq.chirp.userclient.enums.RelationType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.feature.Feature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.sf.jsqlparser.parser.feature.Feature.delete;

@Service
@Slf4j
public class ChirperServiceImpl extends ServiceImpl<ChirperMapper, Chirper> implements ChirperService {
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
    @Resource
    CommunityClient communityClient;

    @Override
    @ParseMentioned
    public ChirperDto save(ChirperDto chirperDto) {
        if (chirperDto.getCommunityId() != null) {
            Map<String, CommunityDto> communityDtoMap = communityClient.fetchMap(List.of(Map.entry(chirperDto.getCommunityId(), chirperDto.getAuthorId()))).getBody();
            assert communityDtoMap.get(StringUtil.combineKey(chirperDto.getCommunityId(), chirperDto.getAuthorId())).getPostable();
        }
        chirperDto = this.getWithPrecondition(chirperDto);
        Chirper chirper = chirperConvertor.dtoToPojo(chirperDto);
        chirper.setId(IdWorker.getId());
        chirper.setConversationId(chirper.getId());
        chirper.setType(ChirperType.ORIGINAL.toString());
        chirperMapper.insert(chirper);
        chirperDto = chirperConvertor.pojoToDto(chirper);
        if (ChirperStatus.DELAY.getStatus() == chirperDto.getStatus()) {
            this.postDelay(chirperDto);
        }
        return chirperDto;
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
        ChirperDto temp = getInteractionStatus(List.of(targetDto), chirperDto.getAuthorId()).getFirst();
        if (!temp.getReplyable()) {
            throw new ChirpException(Code.ERR_BUSINESS, STR."回复失败，该推文仅允许作者\{ReplyRangeEnums.getHint(chirperDto.getReplyRange())}的回复");
        }
        if (chirperDto.getCommunityId() != null) {
            Map<String, CommunityDto> communityDtoMap = communityClient.fetchMap(List.of(Map.entry(chirperDto.getCommunityId(), chirperDto.getAuthorId()))).getBody();
            assert communityDtoMap.get(StringUtil.combineKey(chirperDto.getCommunityId(), chirperDto.getAuthorId())).getPostable();
        }

        chirperDto = this.getWithPrecondition(chirperDto);
        Chirper chirper = chirperConvertor.dtoToPojo(chirperDto);
        chirper.setId(IdWorker.getId());
        chirper.setType(ChirperType.REPLY.toString());
        boolean isInsert = chirperMapper.addReply(chirper) > 0;
        if (!isInsert) {
            throw new ChirpException(Code.ERR_BUSINESS, "回复失败");
        }
        chirperDto = chirperConvertor.pojoToDto(chirper);
        if (ChirperStatus.DELAY.getStatus() == chirperDto.getStatus()) {
            this.postDelay(chirperDto);
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
                .senderId(chirper.getAuthorId())
                .build();
        kafkaTemplate.send(REPLY_MSG_TOPIC, message);
        return chirperDto;
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
                };

                int threadSize = 2;
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
                getInteractionStatus(chirperDtos, userId);
                latch.await();
                chirperDtos.forEach(chirperDto -> {
                    Long id = chirperDto.getId();
                    boolean isLike = ref.likeMap.get(id) != null;
                    boolean isForward = ref.reference.containsValue(Map.entry(id, ChirperType.FORWARD.name()));
                    boolean isQuote = ref.reference.containsValue(Map.entry(id, ChirperType.QUOTE.name()));
                    chirperDto.setIsLike(isLike);
                    chirperDto.setIsForward(isForward);
                    chirperDto.setIsQuote(isQuote);
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
        boolean insert = chirperMapper.insert(chirper) > 0;
        if (insert) {
            Thread.ofVirtual().start(() -> {
                Action<Long, Long> action = new Action<>(
                        ActionTypeEnums.FORWARD.getAction(),
                        DefaultOperation.INCREMENT.getOperation(),
                        userId,
                        chirperId,
                        System.currentTimeMillis()
                );
                kafkaTemplate.send(FORWARD_INCREMENT_COUNT_TOPIC, action);
                NotificationDto messageDto = NotificationDto.builder()
                        .sonEntity(String.valueOf(chirperId))
                        .senderId(userId)
                        .build();
                kafkaTemplate.send(FORWARD_MSG_TOPIC, messageDto);
            });
        } else {
            throw new ChirpException(Code.ERR_SYSTEM, "系统错误，转发失败");
        }

    }

    @Override
    @CacheEvict(cacheNames = "chirper:forward", key = "#chirperId+':'+#userId")
    public boolean cancelForward(Long chirperId, Long userId) {
        long currentTimeMillis = System.currentTimeMillis();
        boolean delete = chirperMapper.delete(new LambdaQueryWrapper<Chirper>()
                .eq(Chirper::getReferencedChirperId, chirperId)
                .eq(Chirper::getAuthorId, userId)
                .eq(Chirper::getType, ChirperType.FORWARD.name())
                .ne(Chirper::getCreateTime, new Timestamp(currentTimeMillis))) > 0;
        if (delete) {
            Action<Long, Long> action = new Action<>(
                    ActionTypeEnums.FORWARD.getAction(),
                    DefaultOperation.DECREMENT.getOperation(),
                    userId,
                    chirperId,
                    currentTimeMillis
            );
            kafkaTemplate.send(FORWARD_INCREMENT_COUNT_TOPIC, action);
        }
        return delete;
    }


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
        boolean isInsert = chirperMapper.addQuote(chirper) > 0;
        if (!isInsert) {
            throw new ChirpException(Code.ERR_BUSINESS, "发布失败");
        }
        chirperDto = chirperConvertor.pojoToDto(chirper);
        if (ChirperStatus.DELAY.getStatus() == chirperDto.getStatus()) {
            this.postDelay(chirperDto);
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
                .senderId(chirper.getAuthorId()).build();
        kafkaTemplate.send(QUOTE_MSG_TOPIC, messageDto);
        return chirperDto;
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
        return this.combine(chirperDtoMap.values());
    }

    @Override
    @Cacheable(value = "chirper:page#4", key = "#chirperQueryDto.page+':'+#chirperQueryDto.chirperId+':'+#chirperQueryDto.userIds+':'+#chirperQueryDto.type+':'+#chirperQueryDto.media+':'+#chirperQueryDto.order")
    public List<ChirperDto> getPage(ChirperQueryDto chirperQueryDto) {
        chirperQueryDto.withDefault();
        Page<Chirper> pageSelector = new Page<>(chirperQueryDto.getPage(), chirperQueryDto.getPageSize(), false);
        LambdaQueryWrapper<Chirper> wrapper = new LambdaQueryWrapper<>();
        if (chirperQueryDto.getChirperId() != null) {
            wrapper.eq(Chirper::getInReplyToChirperId, chirperQueryDto.getChirperId());
        }
        if (!CollectionUtils.isEmpty(chirperQueryDto.getUserIds())) {
            wrapper.in(Chirper::getAuthorId, chirperQueryDto.getUserIds());
        }
        if (ChirperType.find(chirperQueryDto.getType()) != null) {
            wrapper.eq(Chirper::getType, chirperQueryDto.getType());
        }
        if (chirperQueryDto.getMedia() != null && chirperQueryDto.getMedia()) {
            wrapper.isNotNull(Chirper::getMediaKeys);
        }
        if (chirperQueryDto.getCommunityId() != null) {
            wrapper.eq(Chirper::getCommunityId, chirperQueryDto.getCommunityId());
        }
        if (!StringUtil.isBlank(chirperQueryDto.getKeyword())) {
            wrapper.like(Chirper::getText, chirperQueryDto.getKeyword());
        }
        OrderEnum orderEnum = OrderEnum.findAndDefault(chirperQueryDto.getOrder());
        switch (orderEnum) {
            case ASC -> wrapper.orderByAsc(Chirper::getCreateTime);
            case DESC -> wrapper.orderByDesc(Chirper::getCreateTime);
            case HOT -> wrapper.orderByDesc(Chirper::getViewCount);
        }
        //转换为map类型，为下面获取被引用推文准备
        Map<Long, ChirperDto> chirperDtoMap = chirperMapper.selectPage(pageSelector, wrapper).getRecords()
                .stream()
                .filter(chirper -> {
                    //当作者为当前用户时，将延时发布的也查出
                    Long currentUserId = chirperQueryDto.getCurrentUserId();
                    if (currentUserId != null) {
                        if (currentUserId.equals(chirper.getAuthorId()) && ChirperStatus.DELETE.getStatus() != chirper.getStatus()) {
                            return true;
                        }
                    }
                    return ChirperStatus.ACTIVE.getStatus() == chirper.getStatus();
                })
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
        return this.combine(chirperDtoMap.values());
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
    public List<ChirperDto> getBasicInfo(Collection<Long> chirperIds) {
        return chirperMapper.selectList(new LambdaQueryWrapper<Chirper>()
                        .select(Chirper::getId, Chirper::getAuthorId, Chirper::getType, Chirper::getCommunityId)
                        .in(Chirper::getId, chirperIds))
                .stream()
                .map(chirper -> chirperConvertor.pojoToDto(chirper))
                .toList();
    }

    @Override
    public List<ChirperDto> combine(Collection<ChirperDto> chirperDtos) {
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
                                chirperDto.setReferenced(this.combine(List.of(chirperDto.getReferenced())).get(0));
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
                    log.error("", e);
                } finally {
                    latch.countDown();
                }

            });
            Thread.ofVirtual().start(() -> {
                try {
                    Map<Long, List<MediaDto>> medias = mediaClient.getCombine(map).getBody();
                    mediaMap.putAll(medias);
                } catch (Exception e) {
                    log.error("", e);
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
    public List<ChirperDto> getInteractionStatus(List<ChirperDto> chirperDtos, Long userId) {
        Set<Long> authors = chirperDtos.stream()
                .map(ChirperDto::getAuthorId)
                .collect(Collectors.toSet());
        authors.add(userId);
        //获取作者的信息以及与目标用户的关系
        ResponseEntity<List<UserDto>> response = userClient.getUsernameAndRelation(authors, userId);
        Map<Long, UserDto> userDtoMap = new HashMap<>();
        if (response.getStatusCode().is2xxSuccessful()) {
            List<UserDto> userDtoList = response.getBody();
            if (userDtoList != null && !userDtoList.isEmpty()) {
                userDtoMap = userDtoList.stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
            }
        }
        UserDto targetUser = userDtoMap.get(userId);
        for (ChirperDto chirperDto : chirperDtos) {
            boolean isMentioned = false;
            if (targetUser != null && !StringUtil.isBlank(chirperDto.getText())) {
                isMentioned = TextUtil.getIsMentioned(chirperDto.getText(), targetUser.getUsername());
            }
            //如果是该推文的作者，又或者被提及，则全部允许
            if (chirperDto.getAuthorId().equals(userId) || isMentioned) {
                chirperDto.setAllInteractionAllow();
                continue;
            }
            UserDto author = userDtoMap.get(chirperDto.getAuthorId());
            if (RelationType.BLOCK.getRelation() == author.getRelation()) {
                chirperDto.setAllInteractionDeny();
                continue;
            }
            //点赞、转发、引用只受作者是否拉黑用户影响
            chirperDto.setAllInteractionAllow();
            Integer replyRange = chirperDto.getReplyRange();
            replyRange = replyRange != null ? replyRange : ReplyRangeEnums.EVERYONE.getCode();
            if (ReplyRangeEnums.EVERYONE.getCode() == replyRange) {
                chirperDto.setReplyable(true);
            } else {
                    if (ReplyRangeEnums.FOLLOWING.getCode() == replyRange) {
                        boolean able = RelationType.FOLLOWING.getRelation() == author.getRelation();
                        chirperDto.setReplyable(able);
                    } else if (ReplyRangeEnums.MENTION.getCode() == replyRange) {
                        chirperDto.setReplyable(false);
                    }
            }
        }
        return chirperDtos;
    }

    @Override
    public ChirperDto getWithPrecondition(ChirperDto chirperDto) {
        if (chirperDto.isEmpty()) {
            throw new ChirpException(Code.ERR_BUSINESS, "推文内容不能为空");
        }
        ReplyRangeEnums rangeEnums = ReplyRangeEnums.findByCodeWithDefault(chirperDto.getReplyRange());
        chirperDto.setReplyRange(rangeEnums.getCode());
        chirperDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
        Timestamp activeTime = chirperDto.getActiveTime();
        if (activeTime != null && activeTime.getTime() > chirperDto.getCreateTime().getTime()) {
            chirperDto.setStatus(ChirperStatus.DELAY.getStatus());
        } else {
            chirperDto.setActiveTime(chirperDto.getCreateTime());
            chirperDto.setStatus(ChirperStatus.ACTIVE.getStatus());
        }
        return chirperDto;
    }

    @Override
    public void postDelay(ChirperDto chirperDto) {
        String key = STR."\{CacheKey.DELAY_POST_KEY.getKey()}:\{chirperDto.getId()}";
        long between = chirperDto.getActiveTime().getTime() - chirperDto.getCreateTime().getTime();
        if (between > 0) {
            redisTemplate.opsForValue().set(key, 1, Duration.ofMillis(between));
        }
    }

    @Override
    public void activeDelay(Collection<Long> chirperIds) {
        try {
            RetryUtil.doDBRetry(() ->
                    chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                            .set(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                            .ne(Chirper::getStatus, ChirperStatus.DELETE.getStatus())
                            .ne(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                            .eq(Chirper::getStatus, ChirperStatus.DELAY.getStatus())
                            .in(Chirper::getId, chirperIds))
            );
        } catch (ExecutionException e) {
            log.error("激活延时推文时发生无法成功的错误，推文=>{}，错误=>", chirperIds, e);
        } catch (RetryException e) {
            log.error("激活延时推文失败，推文=>{}，错误=>", chirperIds, e);
        }

    }

    @Override
    public void activeDelayAuto() {
        chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                .set(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                .ne(Chirper::getStatus, ChirperStatus.DELETE.getStatus())
                .ne(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                .eq(Chirper::getStatus, ChirperStatus.DELAY.getStatus())
                .le(Chirper::getActiveTime, new Timestamp(System.currentTimeMillis())));
    }

}
