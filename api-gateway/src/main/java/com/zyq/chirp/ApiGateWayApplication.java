package com.zyq.chirp;

import com.zyq.chirp.userclient.client.UserClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan({"com.zyq.chirp.gateway", "com.zyq.chirp.common.redis", "com.zyq.chirp.common.db"})
@MapperScan("com.zyq.chirp.gateway.mapper")
@EnableFeignClients(basePackageClasses = UserClient.class)
@EnableAsync
@EnableScheduling
public class ApiGateWayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGateWayApplication.class, args);
    }
}