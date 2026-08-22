package com.yiliao.notification.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.time.LocalDateTime;

/** message_record：biz_key 唯一（幂等第二层），is_read 为 P3 对上游 DDL 的补充列。 */
@TableName("message_record")
public class MessageRecord extends BaseEntity {

    public static final int TYPE_INTERNAL = 1;
    public static final int TYPE_SMS = 2;

    private Long patientId;
    private Long receiverId;
    private String receiverPhone;
    private Integer msgType;
    private String bizKey;
    private String title;
    private String content;
    private Integer sendStatus;
    private String failReason;
    private LocalDateTime sendTime;
    private Integer isRead = 0;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public Integer getMsgType() { return msgType; }
    public void setMsgType(Integer msgType) { this.msgType = msgType; }
    public String getBizKey() { return bizKey; }
    public void setBizKey(String bizKey) { this.bizKey = bizKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getSendStatus() { return sendStatus; }
    public void setSendStatus(Integer sendStatus) { this.sendStatus = sendStatus; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public LocalDateTime getSendTime() { return sendTime; }
    public void setSendTime(LocalDateTime sendTime) { this.sendTime = sendTime; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
}
