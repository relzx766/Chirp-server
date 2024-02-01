package com.zyq.chirp.authserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.authclient.dto.AuthDto;
import com.zyq.chirp.authserver.domain.enums.AccountTypeEnum;
import com.zyq.chirp.authserver.domain.enums.CacheKey;
import com.zyq.chirp.authserver.service.AuthService;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
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
    @Resource
    UserClient userClient;

    @Override
    public AuthDto login(AuthDto authDto) {
        UserDto userDto = null;
        AccountTypeEnum typeEnum = AccountTypeEnum.findWithDefault(authDto.getAccountType());
        switch (typeEnum) {
            case USERNAME -> userDto = userClient.getDetailByUsername(authDto.getAccount()).getBody();
            case EMAIL -> userDto = userClient.getDetailByEmail(authDto.getAccount()).getBody();
        }
        if (userDto == null) {
            throw new ChirpException(Code.ERR_BUSINESS, "用户不存在");
        } else {
            if (userDto.getPassword().equals(authDto.getPassword())) {
                StpUtil.login(userDto.getId());
                authDto.clearPwd();
                authDto.setToken(StpUtil.getTokenValue());
                return authDto;
            } else {
                throw new ChirpException(Code.ERR_BUSINESS, "账号或密码错误");
            }
        }
    }

    @Override
    public AuthDto signUp(UserDto userDto) {
        ResponseEntity<UserDto> response = userClient.addUser(userDto);
        if (response.getStatusCode().is2xxSuccessful()) {
            return AuthDto.builder().account(userDto.getUsername()).build();
        }
        throw new ChirpException(Code.ERR_BUSINESS, "注册失败");
    }

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
