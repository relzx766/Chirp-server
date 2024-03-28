package com.zyq.chirp.adviceserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zyq.chirp.adviceclient.dto.ChatSettingDto;
import com.zyq.chirp.adviceserver.convertor.ChatConvertor;
import com.zyq.chirp.adviceserver.domain.enums.ChatAllowEnum;
import com.zyq.chirp.adviceserver.domain.pojo.ChatSetting;
import com.zyq.chirp.adviceserver.mapper.ChatSettingMapper;
import com.zyq.chirp.adviceserver.service.ChatSettingService;
import com.zyq.chirp.adviceserver.service.E2EEService;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "setting:chat#2")
public class ChatSettingServiceImpl implements ChatSettingService {
    @Resource
    ChatSettingMapper settingMapper;
    @Resource
    E2EEService e2EEService;
    @Resource
    ChatConvertor chatConvertor;
    String pinnedMapping = "typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler";

    @Override
    public void initOne(Long userId) {
        ChatSetting chatSetting = new ChatSetting();
        chatSetting.setUserId(userId);
        chatSetting.setAllow(ChatAllowEnum.EVERYONE.ordinal());
        settingMapper.insert(chatSetting);
    }

    @Override
    public ChatSettingDto getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        ChatSetting chatSetting = settingMapper.selectOne(new LambdaQueryWrapper<ChatSetting>()
                .eq(ChatSetting::getUserId, userId));
        ChatSettingDto settingDto;
        if (chatSetting == null) {
            this.initOne(userId);
            settingDto = ChatSettingDto.builder()
                    .userId(userId)
                    .allow(ChatAllowEnum.EVERYONE.ordinal())
                    .build();
        } else {
            settingDto = chatConvertor.settingPojoToDto(chatSetting);
        }
        settingDto.setChatReady(e2EEService.getPublicKey(userId) != null);
        return settingDto;
    }

    @Override
    @Cacheable(key = "#userId")
    public ChatSettingDto getByUserId(Long userId) {
        return (ChatSettingDto) this.getByUserId(Set.of(userId)).toArray()[0];
    }

    @Override
    @Cacheable(key = "#userIds")
    public Collection<ChatSettingDto> getByUserId(Set<Long> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            Map<Long, String> publicKey = e2EEService.getPublicKey(userIds.stream().toList());
            ArrayList<Long> noInitUser = new ArrayList<>();
            Map<Long, ChatSettingDto> collect = settingMapper.selectList(new LambdaQueryWrapper<ChatSetting>()
                            .select(ChatSetting::getId, ChatSetting::getUserId, ChatSetting::getAllow)
                            .in(ChatSetting::getUserId, userIds))
                    .stream()
                    .map(chatSetting -> {
                        ChatSettingDto settingDto = chatConvertor.settingPojoToDto(chatSetting);
                        if (settingDto == null) {
                            settingDto = new ChatSettingDto();
                        } else {
                            if (settingDto.getAllow() == null) {
                                settingDto.setAllow(ChatAllowEnum.EVERYONE.ordinal());
                            }
                        }
                        settingDto.setChatReady(publicKey.get(chatSetting.getUserId()) != null);
                        return settingDto;
                    }).collect(Collectors.toMap(ChatSettingDto::getUserId, Function.identity()));
            userIds.forEach(id -> {
                if (!collect.containsKey(id)) {
                    noInitUser.add(id);
                    ChatSettingDto settingDto = ChatSettingDto.builder()
                            .userId(id)
                            .allow(ChatAllowEnum.EVERYONE.ordinal())
                            .build();
                    collect.put(id, settingDto);
                }
            });
            Thread.ofVirtual().start(() -> {
                noInitUser.forEach(this::initOne);
            });
            return collect.values();
        }
        return List.of();
    }

    @Override
    @CacheEvict(key = "#userId")
    public void updateAllow(Long userId, Integer allowEnum) {
        ChatAllowEnum anEnum = ChatAllowEnum.find(allowEnum);
        if (anEnum == null) {
            anEnum = ChatAllowEnum.EVERYONE;
        }
        settingMapper.update(null, new LambdaUpdateWrapper<ChatSetting>()
                .set(ChatSetting::getAllow, anEnum.ordinal())
                .eq(ChatSetting::getUserId, userId));
    }

    @Override
    @CacheEvict(key = "#userId")
    public void updatePinned(Long userId, List<String> conversations) {
        settingMapper.update(null, new LambdaUpdateWrapper<ChatSetting>()
                .set(ChatSetting::getPinned, conversations, pinnedMapping)
                .eq(ChatSetting::getUserId, userId));
    }
}
