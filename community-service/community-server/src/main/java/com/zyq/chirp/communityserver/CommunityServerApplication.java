package com.zyq.chirp.communityserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.zyq.chirp.communityserver.mapper")
@ComponentScan({"com.zyq.chirp.communityserver",
        "com.zyq.chirp.common.redis",
        "com.zyq.chirp.common.db",
        "com.zyq.chirp.common.web"})
public class CommunityServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommunityServerApplication.class, args);
    }
}
