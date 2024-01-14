package com.zyq.chirp.adviceserver.service;

import com.zyq.chirp.adviceclient.dto.ChatSettingDto;

import java.util.Collection;
import java.util.Set;

public interface ChatSettingService {
    void initOne(Long userId);

    ChatSettingDto getCurrentUser();

    ChatSettingDto getByUserId(Long userId);

    Collection<ChatSettingDto> getByUserId(Set<Long> userIds);

    void updateAllow(Long userId, Integer allowEnum);

    void updatePinned(Long userId, String conversationId);
}
