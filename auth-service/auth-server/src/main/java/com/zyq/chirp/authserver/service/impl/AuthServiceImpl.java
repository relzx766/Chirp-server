package com.zyq.chirp.authserver.service.impl;

import com.zyq.chirp.authserver.domain.enums.CacheKey;
import com.zyq.chirp.authserver.service.AuthService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {
    @Resource
    RedisTemplate<String, Object> redisTemplate;
    String ONLINE_CACHE = "online";
    private static final Long DELTA = 1L;
    @Override
    public boolean online(String id) {
        BoundHashOperations<String, String, Integer> operations = redisTemplate.boundHashOps(CacheKey.BOUND_ONLINE_INFO.getKey());
        operations.increment(id, DELTA);
        return true;
    }

    @Override
    public boolean getIsOnline(String id) {
        BoundHashOperations<String, String, Integer> operations = redisTemplate.boundHashOps(CacheKey.BOUND_ONLINE_INFO.getKey());
        Integer connectCount = operations.get(id);
        return connectCount != null && connectCount > 0;
    }


    @Override
    public Map<String, Boolean> getIsOnline(Collection<String> ids) {
        if (ids != null && !ids.isEmpty()) {
            return ids.stream()
                    .map(id -> {
                        Boolean isOnline = this.getIsOnline(id);
                        return Map.entry(id, isOnline);
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return Map.of();
    }

    @Override
    public boolean offline(String id) {
        BoundHashOperations<String, String, Integer> operations = redisTemplate.boundHashOps(CacheKey.BOUND_ONLINE_INFO.getKey());
        operations.increment(id, -DELTA);
        return true;
    }
}
