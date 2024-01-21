package com.zyq.chirp.chirperserver.mq.consumer;

import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.chirperserver.service.ChirperService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
public class DelayConsumer {
    @Resource(name = "redisMessageListenerContainerList")
    List<RedisMessageListenerContainer> redisMessageListenerContainerList;
    @Resource
    ChirperService chirperService;

    @PostConstruct
    public void init() {
        int i = 1;
        PatternTopic patternTopic = new PatternTopic("__keyevent@0__:expired");
        for (RedisMessageListenerContainer listenerContainer : redisMessageListenerContainerList) {
            listenerContainer.addMessageListener(new KeyExpireListener(), patternTopic);
            listenerContainer.start();
            log.info("启用对redis的key过期监听，容器{}状态:active=>{},listen=>{}", i, listenerContainer.isActive(), listenerContainer.isListening());
            i++;
        }
    }

    public class KeyExpireListener implements MessageListener {
        final ConcurrentLinkedQueue<Long> chirperIds = new ConcurrentLinkedQueue<>();
        long mountTime = System.currentTimeMillis();
        long timeout = 2000;
        int sizeLimit = 100;

        @Override
        public void onMessage(Message message, byte[] pattern) {
            String prefix = STR."\{CacheKey.DELAY_POST_KEY.getKey()}:";
            String key = message.toString();
            if (key.startsWith(prefix)) {
                String idStr = key.substring(prefix.length());
                chirperIds.add(Long.valueOf(idStr));
                long currentTime = System.currentTimeMillis();
                if (chirperIds.size() > sizeLimit || currentTime - mountTime > timeout) {
                    synchronized (chirperIds) {
                        ArrayList<Long> chirperIdsCopy = new ArrayList<>(chirperIds);
                        chirperIds.clear();
                        Thread.ofVirtual().start(() -> {
                            chirperService.activeDelay(chirperIdsCopy);
                        });
                        mountTime = currentTime;
                    }
                }
            }
        }
    }

}
