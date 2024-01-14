package com.zyq.chirp.feedserver.service;

import com.zyq.chirp.feedclient.dto.FeedDto;

import java.util.Collection;

public interface FeedService {
    /**
     * 初始化一个feed流
     *
     * @param targetId 目标用户id
     */
    void initFeed(String targetId);
    void addOne(FeedDto feedDto);

    void addFeedBatch(Collection<FeedDto> feedDtos);

    /**
     * 移除指定接收者的指定内容，当用户发生取关等事件时，需要将对应被取关者的内容移除
     *
     * @param receiverId 接收者
     * @param contentIds 内容id
     */
    void removeBatch(String receiverId, Collection<String> contentIds);

    void removeBatch(Collection<FeedDto> feedDtos);

    Collection<FeedDto> getPage(String receiverId, Integer page);

    Collection<FeedDto> getPageByScore(String receiverId, Double score);

    Collection<FeedDto> getRange(String receiverId, Double start, Double end);
}
