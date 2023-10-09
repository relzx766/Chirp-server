package com.zyq.chirp.adviceserver.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaContainerConfig<T> {
    @Resource
    DefaultKafkaConsumerFactory<String, T> consumerFactory;
    @Resource
    KafkaListenerEndpointRegistry registry;

    @Resource
    ConcurrentKafkaListenerContainerFactory<String, T> containerFactory;


    // 创建监听器容器
    public KafkaMessageListenerContainer<String, T> getListenerContainer(
            String id, String topic, String group, BatchMessageListener<String, T> messageListener
    ) {
        ContainerProperties containerProperties = new ContainerProperties(topic);
        containerProperties.setGroupId(group);
        containerProperties.setMessageListener(messageListener);
        containerProperties.setClientId(id);
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        consumerFactory.setBeanName(id);
        KafkaMessageListenerContainer<String, T> container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        return container;
    }

    @Bean
    public Map<String, List<KafkaMessageListenerContainer>> kafkaMessageListenerContainerMap() {
        return new HashMap<>();
    }

}
