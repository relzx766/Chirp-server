package com.zyq.chirp.authserver.service;

import java.util.Collection;
import java.util.Map;

public interface AuthService {
    boolean online(String id);

    boolean getIsOnline(String id);

    Map<String, Boolean> getIsOnline(Collection<String> id);

    boolean offline(String id);
}
