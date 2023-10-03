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

    @Override
    public boolean online(String id) {
        BoundHashOperations<String, String, Boolean> operations = redisTemplate.boundHashOps(CacheKey.BOUND_ONLINE_INFO.getKey());
        operations.put(id, Boolean.TRUE);
        return true;
    }

    @Override
    public boolean getIsOnline(String id) {
        BoundHashOperations<String, String, Boolean> operations = redisTemplate.boundHashOps(CacheKey.BOUND_ONLINE_INFO.getKey());
        Boolean isOnline = operations.get(id);
        return isOnline != null ? isOnline : Boolean.FALSE;
    }


    @Override
    public Map<String, Boolean> getIsOnline(Collection<String> ids) {
        BoundHashOperations<String, String, Boolean> operations = redisTemplate.boundHashOps(CacheKey.BOUND_ONLINE_INFO.getKey());
        if (ids != null && !ids.isEmpty()) {
            return ids.stream()
                    .map(id -> {
                        Boolean isOnline = operations.get(id);
                        return Map.entry(id, isOnline != null ? isOnline : Boolean.FALSE);
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return Map.of();
    }

    @Override
    public boolean offline(String id) {
        BoundHashOperations<String, String, Boolean> operations = redisTemplate.boundHashOps(CacheKey.BOUND_ONLINE_INFO.getKey());
        operations.put(id, Boolean.FALSE);
        return true;
    }
}
