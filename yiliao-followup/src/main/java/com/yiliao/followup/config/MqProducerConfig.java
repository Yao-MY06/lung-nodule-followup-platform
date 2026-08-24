package com.yiliao.followup.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 生产者模板手工装配（rocketmq-spring 2.3.1 在 Boot 3.4 下自动装配的
 * RocketMQTemplate 未注册——监听器 BPP 提前初始化副作用，按属类处理）。
 * 若后续升级 starter 恢复正常，@ConditionalOnMissingBean 保证不冲突。
 */
@Configuration
public class MqProducerConfig {

    private static final Logger log = LoggerFactory.getLogger(MqProducerConfig.class);

    @Bean
    @ConditionalOnMissingBean
    public DefaultMQProducer defaultMQProducer(
            @Value("${rocketmq.name-server}") String nameServer,
            @Value("${rocketmq.producer.group:yiliao-followup-producer}") String group) {
        DefaultMQProducer producer = new DefaultMQProducer(group);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(5000);
        log.info("RocketMQ 生产者手工装配 nameServer={} group={}", nameServer, group);
        return producer;
    }

    @Bean
    @ConditionalOnMissingBean
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer producer) {
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(producer);
        return template;
    }
}
