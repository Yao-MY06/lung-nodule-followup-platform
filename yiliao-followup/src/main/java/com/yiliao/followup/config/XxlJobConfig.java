package com.yiliao.followup.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器（specs/global/30 §9）：默认关闭，联调控制台时开启。
 */
@Configuration
@ConditionalOnProperty(name = "yiliao.xxl-job.enabled", havingValue = "true")
public class XxlJobConfig {

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor(
            @Value("${yiliao.xxl-job.admin-addresses}") String adminAddresses,
            @Value("${yiliao.xxl-job.appname:yiliao-followup}") String appname,
            @Value("${yiliao.xxl-job.port:8084}") int port,
            @Value("${yiliao.xxl-job.log-path:logs/xxl-job}") String logPath) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(7);
        return executor;
    }
}
