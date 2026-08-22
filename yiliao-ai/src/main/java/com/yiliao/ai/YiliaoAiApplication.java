package com.yiliao.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.yiliao.api")
public class YiliaoAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(YiliaoAiApplication.class, args);
    }
}
