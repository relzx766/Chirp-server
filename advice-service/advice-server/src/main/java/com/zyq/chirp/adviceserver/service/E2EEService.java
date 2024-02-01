package com.zyq.chirp.adviceserver.service;

import com.zyq.chirp.adviceclient.dto.E2eeKeypairDto;

import java.util.List;
import java.util.Map;

public interface E2EEService {

    /**
     * 生成密钥对
     *
     * @return
     */
    E2eeKeypairDto getKeypair();

    String savePublicKey(Long userId, String publicKey);

    String getPublicKey(Long userId);

    Map<Long, String> getPublicKey(List<Long> ids);

}
