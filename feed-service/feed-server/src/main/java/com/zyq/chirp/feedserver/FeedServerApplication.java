package com.zyq.chirp.feedserver;

import com.zyq.chirp.chirpclient.client.ChirperClient;
import com.zyq.chirp.userclient.client.UserClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.zyq.chirp.common.redis",
        "com.zyq.chirp.feedserver",
        "com.zyq.chirp.common.web"})
@EnableFeignClients(basePackageClasses = {UserClient.class, ChirperClient.class})
public class FeedServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeedServerApplication.class, args);
    }
}
