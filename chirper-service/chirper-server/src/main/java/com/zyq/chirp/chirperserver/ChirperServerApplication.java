package com.zyq.chirp.chirperserver;

import com.zyq.chirp.communityclient.client.CommunityClient;
import com.zyq.chirp.mediaclient.client.MediaClient;
import com.zyq.chirp.userclient.client.UserClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@ComponentScan({"com.zyq.chirp.chirperserver",
        "com.zyq.chirp.common.redis",
        "com.zyq.chirp.common.db",
        "com.zyq.chirp.common.web"})
@EnableConfigurationProperties
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
@MapperScan("com.zyq.chirp.chirperserver.mapper")
@EnableFeignClients(basePackageClasses = {MediaClient.class, UserClient.class, CommunityClient.class})
public class ChirperServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChirperServerApplication.class, args);
    }
}