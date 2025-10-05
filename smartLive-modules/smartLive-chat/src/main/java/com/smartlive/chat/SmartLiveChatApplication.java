package com.smartlive.chat;

import com.smartLive.common.security.annotation.EnableCustomConfig;
import com.smartLive.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class SmartLiveChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLiveChatApplication.class, args);
    }

}
