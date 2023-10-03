package com.zyq.chirp.feedserver;

import com.zyq.chirp.chirpclient.client.ChirperClient;
import com.zyq.chirp.userclient.client.UserClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackageClasses = {UserClient.class, ChirperClient.class})
public class FeedServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeedServerApplication.class, args);
    }
}
