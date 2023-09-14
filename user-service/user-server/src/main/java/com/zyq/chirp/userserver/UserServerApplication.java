package com.zyq.chirp.userserver;

import com.zyq.chirp.common.mq.DefaultKafkaProducer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@MapperScan("com.zyq.chirp.userserver.mapper")
@EnableConfigurationProperties
@EnableAspectJAutoProxy
@Import(DefaultKafkaProducer.class)
public class UserServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServerApplication.class, args);
    }
}