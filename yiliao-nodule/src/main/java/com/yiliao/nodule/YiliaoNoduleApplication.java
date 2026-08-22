package com.yiliao.nodule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.yiliao.api")
public class YiliaoNoduleApplication {

    public static void main(String[] args) {
        SpringApplication.run(YiliaoNoduleApplication.class, args);
    }
}
