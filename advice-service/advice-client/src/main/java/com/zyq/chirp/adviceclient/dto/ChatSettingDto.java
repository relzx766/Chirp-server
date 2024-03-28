package com.zyq.chirp.adviceclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatSettingDto {
    Long id;
    Long userId;
    Integer allow = 1;
    List<String> pinned;
    /**
     * 公钥上传前无法私聊
     */
    Boolean chatReady = false;
}
