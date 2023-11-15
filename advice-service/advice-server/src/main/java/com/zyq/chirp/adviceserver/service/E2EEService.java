package com.zyq.chirp.adviceserver.service;

import java.util.List;
import java.util.Map;

public interface E2EEService {

    /**
     * 生成密钥对
     *
     * @return
     */
    public String[] generateKeyPair();

    public String savePublicKey(Long userId, String publicKey);

    public String getPublicKey(Long userId);

    public Map<Long, String> getPublicKey(List<Long> ids);

}
