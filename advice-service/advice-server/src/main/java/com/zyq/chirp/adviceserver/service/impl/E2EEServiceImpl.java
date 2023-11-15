package com.zyq.chirp.adviceserver.service.impl;

import com.zyq.chirp.adviceserver.service.E2EEService;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@CacheConfig(cacheNames = "e2ee:key")
public class E2EEServiceImpl implements E2EEService {
    //2040bit的质数p
    private static final String PRIME = "22302432107822061719748063621764213935321086136404756648232040290467958531053881726508051954587263028725537838957825195103443186471963719938402268609875560270812081288136002459660479276513570051715983092852832074685642301824823756759754534044775725082529373907126780181184027180466386479869439081636804657392506585083743682655238181030767471296235621715209793339815228668029991687617988000443586967256232909167516615924920149104062332032173519325570233388286400619263590955622004750472532149256612840523786924835041335581035703199401586432735646926590519720562911900939629514025861882013263997478747524375369851420389";
    private final static String GENERATOR_NUM = "35";
    @Resource
    RedisTemplate redisTemplate;

    @Override
    public String[] generateKeyPair() {
        return new String[]{PRIME, GENERATOR_NUM};
    }

    @Override
    @CachePut(key = "'public:'+#userId")
    public String savePublicKey(Long userId, String publicKey) {
        return publicKey;
    }

    @Override
    @Cacheable(key = "'public:'+#userId")
    public String getPublicKey(Long userId) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Long, String> getPublicKey(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            List<String> strIds = ids.stream().map(id -> STR. "e2ee:key:public:\{ id }" ).toList();
            List<String> values = redisTemplate.opsForValue().multiGet(strIds);
            Map<Long, String> map = new HashMap<>();
            if (values != null) {
                for (int i = 0; i < ids.size(); i++) {
                    map.put(ids.get(i), values.get(i));
                }
            }
            return map;
        }
        return Map.of();
    }
}
