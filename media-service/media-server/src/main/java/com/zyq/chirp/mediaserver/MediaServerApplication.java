package com.zyq.chirp.mediaserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.zyq.chirp.common.redis",
        "com.zyq.chirp.mediaserver",
        "com.zyq.chirp.common.db",
        "com.zyq.chirp.common.web"})
@MapperScan("com.zyq.chirp.mediaserver.mapper")
public class MediaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediaServerApplication.class);
    }
}