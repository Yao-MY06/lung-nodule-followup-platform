package com.yiliao.notification.service;

import com.yiliao.common.mq.constant.Topics;
import com.yiliao.common.mq.event.RemindPayload;
import com.yiliao.common.mq.idempotent.IdempotentChecker;
import com.yiliao.notification.channel.SmsChannel;
import com.yiliao.notification.entity.MessageRecord;
import com.yiliao.notification.mapper.MessageRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRecordMapper mapper;
    @Mock
    private IdempotentChecker checker;
    @Mock
    private SmsChannel smsChannel;

    private MessageService service;

    private final RemindPayload payload =
            new RemindPayload(1001L, 1001L, LocalDate.of(2026, 9, 10), "{\"items\":[\"胸部CT\"]}", 1);

    @BeforeEach
    void setUp() {
        service = new MessageService(mapper, checker, smsChannel, true);
    }

    @Test
    void firstConsumeSendsInternalAndSms() {
        when(checker.tryConsume(eq(Topics.REMIND_DUE), anyString())).thenReturn(true);

        service.consumeRemind(payload, "T1001:REMIND_T7");

        ArgumentCaptor<MessageRecord> captor = ArgumentCaptor.forClass(MessageRecord.class);
        // 短信开启：站内信 + 短信各一条
        verify(mapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(MessageRecord.TYPE_INTERNAL, captor.getAllValues().get(0).getMsgType());
        assertEquals("T1001:REMIND_T7", captor.getAllValues().get(0).getBizKey());
        assertEquals(1, captor.getAllValues().get(0).getSendStatus());
        verify(smsChannel).send(any(), anyString());
    }

    @Test
    void duplicateConsumeDroppedSilently() {
        // 幂等第一层：Redis SETNX 拦截（F2 验证清单 #1：投递 3 次仅发送 1 次）
        when(checker.tryConsume(eq(Topics.REMIND_DUE), anyString())).thenReturn(false);

        service.consumeRemind(payload, "T1001:REMIND_T7");

        verify(mapper, never()).insert(any(MessageRecord.class));
    }

    @Test
    void dbUniqueKeyIsSecondLayer() {
        // Redis 标记成功但并发实例已入库 → DuplicateKeyException 静默处理
        when(checker.tryConsume(eq(Topics.REMIND_DUE), anyString())).thenReturn(true);
        when(mapper.insert(any(MessageRecord.class))).thenThrow(new DuplicateKeyException("dup"));

        service.consumeRemind(payload, "T1001:REMIND_T7");

        verify(checker, never()).release(anyString(), anyString());
    }

    @Test
    void unexpectedFailureReleasesMarkerForRedelivery() {
        when(checker.tryConsume(eq(Topics.REMIND_DUE), anyString())).thenReturn(true);
        when(mapper.insert(any(MessageRecord.class))).thenThrow(new RuntimeException("db down"));

        try {
            service.consumeRemind(payload, "T1001:REMIND_T7");
        } catch (RuntimeException expected) {
            // 向上抛出触发 MQ 重试
        }
        verify(checker).release(Topics.REMIND_DUE, "T1001:REMIND_T7");
    }

    @Test
    void smsDisabledOnlyInternal() {
        MessageService noSms = new MessageService(mapper, checker, smsChannel, false);
        when(checker.tryConsume(eq(Topics.REMIND_DUE), anyString())).thenReturn(true);
        lenient().when(smsChannel.send(any(), anyString())).thenReturn(true);

        noSms.consumeRemind(payload, "T1001:REMIND_T3");

        verify(mapper, org.mockito.Mockito.times(1)).insert(any(MessageRecord.class));
        verify(smsChannel, never()).send(any(), anyString());
    }

    @Test
    void internalMessageIdempotentReturnsExisting() {
        when(checker.tryConsume(eq("INTERNAL"), anyString())).thenReturn(false);
        MessageRecord existing = new MessageRecord();
        existing.setId(9L);
        existing.setBizKey("biz-1");
        when(mapper.selectOne(any())).thenReturn(existing);

        Long id = service.sendInternal(7L, "biz-1", "t", "c");

        assertEquals(9L, id);
        verify(mapper, never()).insert(any(MessageRecord.class));
    }
}
