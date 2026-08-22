package com.yiliao.notification.controller;

import com.yiliao.api.notify.NotifyApi;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.page.PageQuery;
import com.yiliao.common.core.page.PageResult;
import com.yiliao.common.core.result.Result;
import com.yiliao.notification.entity.MessageRecord;
import com.yiliao.notification.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息接口 + NotifyApi 契约实现（specs/modules/notification.md §3）。
 */
@RestController
public class MessageController implements NotifyApi {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(summary = "我的消息（站内信）")
    @GetMapping("/api/notify/messages")
    public Result<PageResult<MessageRecord>> myMessages(
            @RequestHeader(YiliaoConstants.HEADER_USER_ID) Long userId, @Valid PageQuery query) {
        return Result.ok(messageService.myMessages(userId, query));
    }

    @Operation(summary = "标记已读（仅本人消息）")
    @PutMapping("/api/notify/messages/{id}/read")
    public Result<Void> markRead(@PathVariable Long id,
                                 @RequestHeader(YiliaoConstants.HEADER_USER_ID) Long userId) {
        messageService.markRead(id, userId);
        return Result.ok();
    }

    @Override
    public Result<Long> sendInternalMessage(InternalMessageRequest request) {
        return Result.ok(messageService.sendInternal(request.receiverId(), request.bizKey(),
                request.title(), request.content()));
    }
}
