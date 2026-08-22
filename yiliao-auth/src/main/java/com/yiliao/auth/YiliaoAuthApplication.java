package com.yiliao.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableFeignClients(basePackages = "com.yiliao.api")
public class YiliaoAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(YiliaoAuthApplication.class, args);
    }
}
