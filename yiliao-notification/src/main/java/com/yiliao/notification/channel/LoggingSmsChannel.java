package com.yiliao.notification.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认短信实现：仅日志（阿里云/腾讯云 SDK 对接后替换， specs/modules/notification.md §7）。
 */
@Component
@ConditionalOnMissingBean(SmsChannel.class)
public class LoggingSmsChannel implements SmsChannel {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsChannel.class);

    @Override
    public boolean send(String phone, String content) {
        log.info("[SMS-STUB] phone={} content={}", phone == null ? "" : phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"), content);
        return true;
    }
}
