package com.yiliao.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 附件服务入口（specs/modules/file.md）：MinIO 预签名上传/下载，无数据库。 */
@SpringBootApplication
public class YiliaoFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(YiliaoFileApplication.class, args);
    }
}
