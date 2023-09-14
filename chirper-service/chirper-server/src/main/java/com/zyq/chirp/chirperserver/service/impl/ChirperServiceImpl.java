package com.zyq.chirp.chirperserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.MessageType;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.aspect.Statistic;
import com.zyq.chirp.chirperserver.convertor.ChirperConvertor;
import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.chirperserver.domain.enums.ChirperStatus;
import com.zyq.chirp.chirperserver.domain.enums.ChirperType;
import com.zyq.chirp.chirperserver.domain.pojo.Chirper;
import com.zyq.chirp.chirperserver.mapper.ChirperMapper;
import com.zyq.chirp.chirperserver.mq.producer.ChirperProducer;
import com.zyq.chirp.chirperserver.service.ChirperService;
import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import com.zyq.chirp.common.util.CacheUtil;
import com.zyq.chirp.common.util.PageUtil;
import com.zyq.chirp.mediaclient.client.MediaClient;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChirperServiceImpl implements ChirperService {
    @Resource
    ChirperMapper chirperMapper;
    @Resource
    RedisTemplate redisTemplate;
    @Resource
    ChirperConvertor chirperConvertor;
    @Resource
    MediaClient mediaClient;
    @Resource
    ObjectMapper objectMapper;
    @Resource
    ChirperProducer<SiteMessageDto> chirperProducer;
    @Value("${default-config.page-size}")
    Integer pageSize;
    @Value("${mq.topic.site-message.forward}")
    String forwardTopic;
    @Value("${mq.topic.site-message.quote}")
    String quoteTopic;
    @Value("${mq.topic.site-message.reply}")
    String replyTopic;
    @Value("${mq.topic.chirper-delay-post}")
    String delayPostTopic;
    Integer expire = 6;

    @Override
    public ChirperDto save(ChirperDto chirperDto) {
        Chirper chirper = chirperConvertor.dtoToPojo(chirperDto);
        chirper.setId(IdWorker.getId());
        chirper.setConversationId(chirper.getId());
        chirper.setStatus(ChirperStatus.ACTIVE.getStatus());
        chirper.setType(ChirperType.ORIGINAL.toString());
        chirperMapper.insert(chirper);
        return chirperConvertor.pojoToDto(chirper);
    }

    @Override
    @Transactional
    @Statistic(id = "#chirperDto.inReplyToChirperId", key = CacheKey.VIEW_COUNT_BOUND_KEY)
    public ChirperDto reply(ChirperDto chirperDto) {
        if (null == chirperDto.getInReplyToChirperId()) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供回复对象");
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
        SiteMessageDto siteMessageDto = new SiteMessageDto(chirperDto.getAuthorId(), chirperDto.getInReplyToChirperId(), MessageType.REPLY.name());
        chirperProducer.send(replyTopic, siteMessageDto);
        return chirperConvertor.pojoToDto(chirper);
    }

    /**
     * 定时发布
     *
     * @param chirperDto
     * @param delay
     * @return
     */
    @Override
    public ChirperDto delayPost(ChirperDto chirperDto, Long delay) {
        Chirper chirper = chirperConvertor.dtoToPojo(chirperDto);
        chirper.setStatus(ChirperStatus.DELAY.getStatus());
        chirper.setType(ChirperType.ORIGINAL.toString());
        chirper.setCreateTime(new Timestamp(System.currentTimeMillis()));
        chirperMapper.insert(chirper);
        chirperDto = chirperConvertor.pojoToDto(chirper);
        return chirperDto;
    }


    @Override
    @Transactional
    @Statistic(id = "#chirperId",
            key = {CacheKey.VIEW_COUNT_BOUND_KEY, CacheKey.FORWARD_COUNT_BOUND_KEY})
    public void forward(Long chirperId, Long userId) {
        BoundHashOperations<String, String, Integer> operations = redisTemplate.boundHashOps(CacheKey.FORWARD_INFO_BOUND_KEY.getKey());
        Boolean notExists = operations.putIfAbsent(CacheUtil.combineKey(chirperId, userId), 1);
        if (notExists) {
            Chirper chirper = new Chirper();
            chirper.setAuthorId(userId);
            chirper.setReferencedChirperId(chirperId);
            chirper.setType(ChirperType.FORWARD.toString());
            chirper.setStatus(ChirperStatus.ACTIVE.getStatus());
            chirper.setCreateTime(new Timestamp(System.currentTimeMillis()));
            boolean isInsert = chirperMapper.insert(chirper) > 0;
            if (!isInsert) {
                operations.delete(CacheUtil.combineKey(chirperId, userId));

            }
        } else {
            boolean isUpdate = chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                    .set(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus())
                    .eq(Chirper::getReferencedChirperId, chirperId)
                    .eq(Chirper::getAuthorId, userId)
                    .eq(Chirper::getType, ChirperType.FORWARD.name())) > 0;
            if (!isUpdate) {
                throw new ChirpException(Code.ERR_BUSINESS, "转发失败");
            }
        }
        SiteMessageDto messageDto = new SiteMessageDto(userId, chirperId, MessageType.FORWARD.name());
        chirperProducer.avoidRedundancySend(CacheUtil.combineKey(CacheUtil.combineKey(chirperId, userId)),
                forwardTopic, messageDto, Duration.ofHours(expire));
    }

    @Override
    @Statistic(id = "#chirperId", delta = -1, key = CacheKey.FORWARD_COUNT_BOUND_KEY)
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
    public List<ChirperDto> getByReference(List<ChirperDto> chirperDtos) {
        if (chirperDtos == null || chirperDtos.isEmpty()) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供推文信息");
        }
        return chirperMapper.getIdByReferenceAndAuthor(
                        chirperDtos.stream().map(chirperDto -> chirperConvertor.dtoToPojo(chirperDto)).toList(),
                        ChirperType.FORWARD.name()
                ).stream()
                .map(chirper -> chirperConvertor.pojoToDto(chirper))
                .toList();
    }


    @Override
    @Transactional
    @Statistic(id = "#chirperDto.referencedChirperId", key = CacheKey.VIEW_COUNT_BOUND_KEY)
    public ChirperDto quote(ChirperDto chirperDto) {
        Chirper chirper = chirperConvertor.dtoToPojo(chirperDto);
        chirper.setId(IdWorker.getId());
        chirper.setConversationId(chirper.getId());
        chirper.setType(ChirperType.QUOTE.toString());
        chirper.setStatus(ChirperStatus.ACTIVE.getStatus());
        chirper.setCreateTime(new Timestamp(System.currentTimeMillis()));
        boolean isInsert = chirperMapper.insert(chirper) > 0;

        boolean isSet = chirperMapper.update(null, new LambdaUpdateWrapper<Chirper>()
                .setSql("quote_count=quote_count+1")
                .eq(Chirper::getId, chirper.getReferencedChirperId())) > 0;
        if (!isInsert || !isSet) {
            throw new ChirpException(Code.ERR_BUSINESS, "发布失败");
        }
        SiteMessageDto messageDto = new SiteMessageDto(chirper.getAuthorId(), chirper.getReferencedChirperId(), MessageType.QUOTE.name());
        chirperProducer.send(quoteTopic, messageDto);
        return chirperConvertor.pojoToDto(chirper);
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
    @Statistic(id = "#chirperId", key = CacheKey.VIEW_COUNT_BOUND_KEY)
    public ChirperDto getById(Long chirperId, Long currentUserId) {
        if (chirperId == null) {
            throw new ChirpException(Code.ERR_BUSINESS, "未指定推文");
        }
        return Optional.ofNullable(currentUserId)
                .map(userId -> chirperConvertor.voToDto(chirperMapper.getById(chirperId, currentUserId)))
                .orElseGet(() -> {
                    Chirper chirper = chirperMapper.selectOne(new LambdaQueryWrapper<Chirper>()
                            .eq(Chirper::getId, chirperId)
                            .eq(Chirper::getStatus, ChirperStatus.ACTIVE.getStatus()));
                    return chirperConvertor.pojoToDto(chirper);
                });
    }

    @Override
    public List<ChirperDto> getPage(Integer page, Long userId) {
        int offset = PageUtil.getOffset(page, pageSize);
        return chirperMapper.getLimit(userId, offset, pageSize)
                .stream()
                .map(chirperVo -> chirperConvertor.voToDto(chirperVo))
                .toList();
    }

    /**
     * @param keyword
     * @param page
     * @param currentUserId
     * @param isMedia
     * @return 关键词为空或null返回空列表
     */
    @Override
    public List<ChirperDto> search(String keyword, Integer page, Long currentUserId, Boolean isMedia) {
        int offset = PageUtil.getOffset(page, pageSize);
        return Optional.ofNullable(keyword)
                .map(matchWord ->
                        chirperMapper.getMatchLimit(keyword,
                                        currentUserId,
                                        offset,
                                        pageSize,
                                        isMedia)
                                .stream()
                                .map(chirperVo -> chirperConvertor.voToDto(chirperVo))
                                .toList())
                .orElse(new ArrayList<>());
    }

    /**
     * 获取推文下的一级评论
     *
     * @param chirperId
     * @param page
     * @param currentUserId 访问者，可传入空值
     * @return
     */
    @Override
    public List<ChirperDto> getChildChirper(Long chirperId, Integer page, Long currentUserId) {
        if (chirperId == null) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供推文id");
        }
        int offset = PageUtil.getOffset(page, pageSize);
        return chirperMapper.getChildrenChirperLimit(chirperId, currentUserId, offset, pageSize)
                .stream()
                .map(chirperVo -> chirperConvertor.voToDto(chirperVo))
                .toList();
    }

    @Override
    public List<ChirperDto> getByAuthorId(Long authorId, Integer page, Long currentUserId) {
        return Optional.ofNullable(authorId)
                .map(author ->
                        chirperMapper.getByAuthorLimit(author, currentUserId,
                                        PageUtil.getOffset(page, pageSize), pageSize)
                                .stream()
                                .map(chirperVo -> chirperConvertor.voToDto(chirperVo))
                                .toList())
                .orElseThrow(() -> new ChirpException(Code.ERR_BUSINESS, "未指定博主"));
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
    public Long getAuthorIdByChirperId(Long chirperId) {
        return chirperMapper.selectOne(new LambdaQueryWrapper<Chirper>()
                .select(Chirper::getAuthorId)
                .eq(Chirper::getId, chirperId)).getAuthorId();
    }

    /**
     * 获取推文的简略信息
     *
     * @param chirperId
     * @return
     */
    @Override
    public ChirperDto getShort(Long chirperId) {
        return chirperConvertor.pojoToDto(chirperMapper.selectOne(new LambdaQueryWrapper<Chirper>()
                .select(Chirper::getId, Chirper::getAuthorId, Chirper::getText, Chirper::getMediaKeys)
                .eq(Chirper::getId, chirperId)));
    }

    @Override
    public List<ChirperDto> getShort(Collection<Long> chirperIds) {
        return chirperMapper.selectList(new LambdaQueryWrapper<Chirper>()
                        .select(Chirper::getId, Chirper::getAuthorId, Chirper::getText, Chirper::getMediaKeys)
                        .in(Chirper::getId, chirperIds))
                .stream()
                .map(chirper -> chirperConvertor.pojoToDto(chirper))
                .toList();
    }

    @Override
    public List<ChirperDto> combineWithMedia(List<ChirperDto> chirperDtos) {
        Map<Long, List<Integer>> map = chirperDtos.stream()
                .map(chirperDto -> {
                    try {
                        List<Integer> mediaKeys = objectMapper.readValue(chirperDto.getMediaKeys(), new TypeReference<>() {
                        });
                        return Map.entry(chirperDto.getId(), mediaKeys);
                    } catch (JsonProcessingException | IllegalArgumentException e) {
                        log.warn("媒体key转换失败;{}", e.getMessage());
                        return Map.entry(chirperDto.getId(), List.<Integer>of());
                    }
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<Long, List<MediaDto>> mediaMap = mediaClient.getCombine(map).getBody();
        if (mediaMap != null && !mediaMap.isEmpty()) {
            chirperDtos.forEach(chirperDto -> {
                try {
                    String json = objectMapper.writeValueAsString(mediaMap.get(chirperDto.getId()));
                    chirperDto.setMediaKeys(json);
                } catch (JsonProcessingException e) {
                    log.warn("媒体值转换为json失败{}", e.getMessage());
                    chirperDto.setMediaKeys("");
                }
            });
        }
        return chirperDtos;
    }
}
