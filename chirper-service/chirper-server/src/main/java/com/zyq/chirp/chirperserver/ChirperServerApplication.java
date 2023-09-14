package com.zyq.chirp.chirperserver;

import com.zyq.chirp.mediaclient.client.MediaClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableConfigurationProperties
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
@MapperScan("com.zyq.chirp.chirperserver.mapper")
@EnableFeignClients(basePackageClasses = {MediaClient.class})
public class ChirperServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChirperServerApplication.class, args);
    }
}