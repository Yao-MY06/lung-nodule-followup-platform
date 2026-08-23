package com.yiliao.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 统计驾驶舱服务（specs/modules/statistics.md）：预聚合快照 + 只读查询，无 Feign 依赖。
 */
@SpringBootApplication
public class YiliaoStatisticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(YiliaoStatisticsApplication.class, args);
    }
}
