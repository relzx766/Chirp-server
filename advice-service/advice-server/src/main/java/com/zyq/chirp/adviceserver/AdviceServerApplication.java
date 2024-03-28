package com.zyq.chirp.adviceserver;

import com.zyq.chirp.authclient.client.AuthClient;
import com.zyq.chirp.chirpclient.client.ChirperClient;
import com.zyq.chirp.communityclient.client.CommunityClient;
import com.zyq.chirp.userclient.client.UserClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.zyq.chirp.adviceserver",
        "com.zyq.chirp.common.redis",
        "com.zyq.chirp.common.db",
        "com.zyq.chirp.common.web"})
@EnableFeignClients(basePackageClasses = {ChirperClient.class, UserClient.class, AuthClient.class, CommunityClient.class})
@EnableCaching
@MapperScan("com.zyq.chirp.adviceserver.mapper")
public class AdviceServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdviceServerApplication.class);
    }
}