package com.zyq.chirp.common.redis.config;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;
import java.util.Set;

@Configuration
@Slf4j
public class RedisCommonConfig {
    @Resource
    LettuceConnectionFactory connectionFactory;
    @Value("${spring.data.redis.password}")
    private char[] password;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        StringRedisSerializer serializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setKeySerializer(serializer);
        redisTemplate.setHashKeySerializer(serializer);
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisClusterClient redisClusterClient() {
        Set<RedisNode> nodes = connectionFactory.getClusterConfiguration().getClusterNodes();
        List<RedisURI> redisURIS = nodes.stream()
                .map(node -> RedisURI.builder()
                        .withHost(node.getHost())
                        .withPort(node.getPort())
                        .withPassword(password)
                        .build())
                .toList();
        return RedisClusterClient.create(redisURIS);
    }

    @Bean
    @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }

    @Bean(name = "redisMessageListenerContainerList")
    public List<RedisMessageListenerContainer> redisMessageListenerContainerList() {
        Set<RedisNode> nodes = connectionFactory.getClusterConfiguration().getClusterNodes();
        return nodes.stream()
                .map(redisNode -> {
                    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
                    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redisNode.getHost(), redisNode.getPort());
                    configuration.setPassword(password);
                    LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
                    factory.afterPropertiesSet();
                    log.info("节点{}:{}连接状态==>{}", redisNode.getHost(), redisNode.getPort(), !factory.getConnection().isClosed());
                    container.setConnectionFactory(factory);
                    container.afterPropertiesSet();
                    log.info("容器状态:active=>{},running=>{},auto start=>{}", container.isActive(), container.isRunning(), container.isAutoStartup());
                    return container;
                }).toList();
    }

}
