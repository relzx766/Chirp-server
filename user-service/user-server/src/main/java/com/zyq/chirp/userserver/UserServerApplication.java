package com.zyq.chirp.userserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@ComponentScan({"com.zyq.chirp.userserver",
        "com.zyq.chirp.common.redis",
        "com.zyq.chirp.common.db",
        "com.zyq.chirp.common.web"})
@MapperScan("com.zyq.chirp.userserver.mapper")
@EnableConfigurationProperties
@EnableAspectJAutoProxy
public class UserServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServerApplication.class, args);
    }
}