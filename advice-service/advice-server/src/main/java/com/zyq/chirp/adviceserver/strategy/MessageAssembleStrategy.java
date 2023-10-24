package com.zyq.chirp.adviceserver.strategy;

import com.zyq.chirp.adviceclient.dto.SiteMessage;

import java.util.List;

public interface MessageAssembleStrategy<T extends SiteMessage> {
    public List<T> assemble(List<T> messages);
}
