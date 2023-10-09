package com.zyq.chirp.chirperserver.task;

import com.zyq.chirp.chirpclient.dto.LikeDto;
import com.zyq.chirp.chirperserver.convertor.LikeConvertor;
import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.chirperserver.domain.enums.LikeType;
import com.zyq.chirp.chirperserver.service.LikeService;
import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import com.zyq.chirp.common.util.CacheUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LikeServerTask {
    @Resource
    RedisTemplate redisTemplate;
    @Resource
    LikeService likeService;
    @Resource
    LikeConvertor likeConvertor;
    @Value("${default-config.per-save-like-size}")
    Long saveLimit;

    @Async
    @Scheduled(fixedDelay = 4000)
    public void saveToDbTask() throws InterruptedException {
        BoundHashOperations<String, String, LikeDto> operations = redisTemplate.boundHashOps(CacheKey.LIKE_INFO_BOUND_KEY.getKey());
        Map<Integer, List<LikeDto>> likeMap = Objects.requireNonNull(operations.keys())
                .stream()
                .limit(saveLimit)
                .map(operations::get)
                .collect(
                        Collectors
                                .groupingBy(likeDto ->
                                        Optional.ofNullable(likeDto.getType())
                                                .orElseThrow(() -> new ChirpException(Code.ERR_SYSTEM, "缓存出错"))));
        if (!likeMap.isEmpty()) {
            CompletableFuture.runAsync(() -> saveAddLike(likeMap.get(LikeType.ADD.getType())));
            CompletableFuture.runAsync(() -> saveCancelLike(likeMap.get(LikeType.CANCEL.getType())));
        }
    }

    @Transactional
    public void saveAddLike(List<LikeDto> likeDtos) {
        log.info("持久化新增点赞 start----");
        if (likeDtos != null && !likeDtos.isEmpty()) {
            BoundHashOperations<String, String, LikeDto> operations = redisTemplate.boundHashOps(CacheKey.LIKE_INFO_BOUND_KEY.getKey());
            try {
                boolean flag = likeService.addList(
                        likeDtos.stream()
                                .map(likeDto -> likeConvertor.dtoToPojo(likeDto)).collect(Collectors.toList()));
                if (flag) {
                    Object[] keys = likeDtos.stream()
                            .map(likeDto ->
                                    CacheUtil.combineKey(likeDto.getChirperId(), likeDto.getUserId(), likeDto.getType()))
                            .toArray();
                    operations.delete(keys);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("----end 持久化新增点赞");
    }

    @Transactional
    public void saveCancelLike(List<LikeDto> likeDtos) {
        log.info("持久化取消点赞---");
        if (likeDtos != null && !likeDtos.isEmpty()) {
            BoundHashOperations<String, String, LikeDto> operations = redisTemplate.boundHashOps(CacheKey.LIKE_INFO_BOUND_KEY.getKey());
            try {
                boolean flag = likeService.deleteList(
                        likeDtos.stream()
                                .map(likeDto -> likeConvertor.dtoToPojo(likeDto)).toList());
                if (flag) {
                    Object[] keys = likeDtos.stream()
                            .map(likeDto ->
                                    CacheUtil.combineKey(likeDto.getChirperId(), likeDto.getUserId(), likeDto.getType()))
                            .toArray();

                    operations.delete(keys);
                }
            } catch (Exception e) {
                saveAddLike(likeDtos);
            }
        }
        log.info("----end 持久化取消点赞");


    }

    @Async
    @Scheduled(fixedDelay = 4000)
    @Transactional
    public void updateLikeCount() {
        log.info("持久化点赞数量 start----");
        BoundHashOperations<String, String, Integer> countOperations = redisTemplate.boundHashOps(CacheKey.LIKE_COUNT_BOUND_KEY.getKey());
        Objects.requireNonNull(countOperations.keys())
                .stream()
                .limit(saveLimit)
                .forEach(id -> {
                    System.out.println(id);
                    Integer delta = countOperations.get(id);
                    boolean flag = likeService.updateLikeCount(Long.valueOf(id), Optional.ofNullable(delta).orElse(0));
                    if (flag) {
                        countOperations.delete(id);
                    }
                });
        log.info("----end 持久化点赞数量");
    }
}
