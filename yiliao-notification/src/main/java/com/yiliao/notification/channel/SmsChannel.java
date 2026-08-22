package com.yiliao.notification.channel;

/**
 * 短信渠道（specs/modules/notification.md §4.2）：站内信必达不受短信影响；
 * 厂商对接前用日志实现，发送失败由调用方记录，不抛异常。
 */
public interface SmsChannel {

    /** @return true=已受理；false=渠道不可用（不抛异常，失败信息走 send_status） */
    boolean send(String phone, String content);
}
