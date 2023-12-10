package com.zyq.chirp.authserver;

import com.zyq.chirp.userclient.client.UserClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.zyq.chirp.authserver",
        "com.zyq.chirp.common.redis",
        "com.zyq.chirp.common.db",
        "com.zyq.chirp.common.web"})
@EnableFeignClients(basePackageClasses = UserClient.class)
public class AuthServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }
}