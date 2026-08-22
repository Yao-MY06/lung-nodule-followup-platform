package com.yiliao.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiliao.common.core.page.PageQuery;
import com.yiliao.common.core.page.PageResult;
import com.yiliao.common.data.page.PageResults;
import com.yiliao.common.mq.constant.Topics;
import com.yiliao.common.mq.event.RemindPayload;
import com.yiliao.common.mq.idempotent.IdempotentChecker;
import com.yiliao.notification.channel.SmsChannel;
import com.yiliao.notification.entity.MessageRecord;
import com.yiliao.notification.mapper.MessageRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 消息发送（specs/modules/notification.md §4）：幂等三保险 =
 * ① Redis SETNX(bizKey) ② message_record 唯一键 ③ 渠道发送只发一次。
 * 站内信本地事务即达；短信可选渠道失败不影响站内信。
 */
@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRecordMapper mapper;
    private final IdempotentChecker idempotentChecker;
    private final SmsChannel smsChannel;
    private final boolean smsEnabled;

    public MessageService(MessageRecordMapper mapper, IdempotentChecker idempotentChecker,
                          SmsChannel smsChannel,
                          @Value("${yiliao.notify.sms-enabled:false}") boolean smsEnabled) {
        this.mapper = mapper;
        this.idempotentChecker = idempotentChecker;
        this.smsChannel = smsChannel;
        this.smsEnabled = smsEnabled;
    }

    /** 消费 REMIND_DUE：重复投递直接丢弃（specs/flows/F2 验证清单 #1）。 */
    public void consumeRemind(RemindPayload payload, String bizKey) {
        if (!idempotentChecker.tryConsume(Topics.REMIND_DUE, bizKey)) {
            log.info("重复提醒消息丢弃 bizKey={}", bizKey);
            return;
        }
        try {
            String title = remindTitle(payload.remindType());
            String content = "您有一项 " + payload.planDate() + " 的复查预约（"
                    + itemsBrief(payload.items()) + "），请按时前往。";
            insertInternal(payload.patientId(), bizKey, title, content);
            if (smsEnabled) {
                sendSmsBestEffort(payload.patientId(), bizKey + ":SMS", content);
            }
        } catch (DuplicateKeyException e) {
            // DB 唯一键兜底：并发下另一实例已处理
            log.info("DB 唯一键拦截重复消息 bizKey={}", bizKey);
        } catch (Exception e) {
            // 释放幂等标记，允许 MQ 重投重试
            idempotentChecker.release(Topics.REMIND_DUE, bizKey);
            throw e;
        }
    }

    /** NotifyApi 即时站内信（同一幂等路径）。 */
    public Long sendInternal(Long receiverId, String bizKey, String title, String content) {
        if (!idempotentChecker.tryConsume("INTERNAL", bizKey)) {
            return existingId(bizKey);
        }
        try {
            return insertInternal(receiverId, bizKey, title, content);
        } catch (DuplicateKeyException e) {
            return existingId(bizKey);
        }
    }

    public PageResult<MessageRecord> myMessages(Long receiverId, PageQuery query) {
        return PageResults.of(mapper.selectPage(Page.of(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<MessageRecord>()
                        .eq(MessageRecord::getReceiverId, receiverId)
                        .eq(MessageRecord::getMsgType, MessageRecord.TYPE_INTERNAL)
                        .orderByDesc(MessageRecord::getId)));
    }

    public void markRead(Long id, Long receiverId) {
        MessageRecord record = mapper.selectById(id);
        if (record != null && receiverId.equals(record.getReceiverId())) {
            record.setIsRead(1);
            mapper.updateById(record);
        }
    }

    private Long insertInternal(Long receiverId, String bizKey, String title, String content) {
        MessageRecord record = new MessageRecord();
        record.setReceiverId(receiverId);
        record.setMsgType(MessageRecord.TYPE_INTERNAL);
        record.setBizKey(bizKey);
        record.setTitle(title);
        record.setContent(content);
        record.setSendStatus(1);
        record.setSendTime(LocalDateTime.now());
        mapper.insert(record);
        return record.getId();
    }

    private void sendSmsBestEffort(Long patientId, String bizKey, String content) {
        try {
            MessageRecord sms = new MessageRecord();
            sms.setPatientId(patientId);
            sms.setMsgType(MessageRecord.TYPE_SMS);
            sms.setBizKey(bizKey);
            sms.setTitle("复查提醒");
            sms.setContent(content);
            boolean ok = smsChannel.send(null, content);
            sms.setSendStatus(ok ? 1 : 2);
            if (!ok) {
                sms.setFailReason("渠道不可用");
            }
            sms.setSendTime(LocalDateTime.now());
            mapper.insert(sms);
        } catch (Exception e) {
            log.warn("短信发送失败 bizKey={}（不影响站内信）", bizKey, e);
        }
    }

    private Long existingId(String bizKey) {
        MessageRecord existing = mapper.selectOne(new LambdaQueryWrapper<MessageRecord>()
                .eq(MessageRecord::getBizKey, bizKey).last("LIMIT 1"));
        return existing == null ? null : existing.getId();
    }

    private static String remindTitle(Integer remindType) {
        int type = remindType == null ? 3 : remindType;
        return switch (type) {
            case 1 -> "复查提醒（还有 7 天）";
            case 2 -> "复查提醒（还有 3 天）";
            default -> "复查提醒（明天）";
        };
    }

    private static String itemsBrief(String itemsJson) {
        return itemsJson == null || itemsJson.isBlank() ? "复查项目" : itemsJson;
    }
}
