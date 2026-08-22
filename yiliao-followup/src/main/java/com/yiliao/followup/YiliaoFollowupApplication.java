package com.yiliao.followup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.yiliao.api")
public class YiliaoFollowupApplication {

    public static void main(String[] args) {
        SpringApplication.run(YiliaoFollowupApplication.class, args);
    }
}
