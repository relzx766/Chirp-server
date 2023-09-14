package com.zyq.chirp.mediaserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zyq.chirp.mediaserver.mapper")
public class MediaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediaServerApplication.class);
    }
}