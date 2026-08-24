-- yiliao_notify 库（specs/modules/notification.md §2；DDL 源自 make/技术设计文档 §10，补 create_by/update_by）
--宣教/问卷 P3b 再实现代码，本期仅建表

USE yiliao_notify;

CREATE TABLE IF NOT EXISTS message_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT,
  receiver_id BIGINT COMMENT '站内信接收人（患者或医生）',
  receiver_phone VARCHAR(20),
  msg_type TINYINT COMMENT '1站内信 2短信 3模板消息',
  biz_key VARCHAR(64) NOT NULL UNIQUE COMMENT '幂等键=taskId:remindType 或事件标识',
  title VARCHAR(128),
  content VARCHAR(1000),
  send_status TINYINT COMMENT '0待发 1成功 2失败',
  fail_reason VARCHAR(255),
  send_time DATETIME,
  is_read TINYINT DEFAULT 0 COMMENT '站内信已读标记（P3 对上游 DDL 的补充列）',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_receiver(receiver_id), KEY idx_patient(patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送记录（幂等对账）';

CREATE TABLE IF NOT EXISTS education_article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128),
  category VARCHAR(32) COMMENT '筛查/随访/术后/用药/营养',
  content TEXT,
  target_tags VARCHAR(255) COMMENT '定向推送标签',
  publish_status TINYINT,
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康宣教（P3b）';

CREATE TABLE IF NOT EXISTS questionnaire (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128),
  scale_type VARCHAR(32) COMMENT 'MDASI等',
  questions_json JSON,
  score_rule_json JSON,
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访问卷模板（P3b）';

CREATE TABLE IF NOT EXISTS questionnaire_answer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  questionnaire_id BIGINT, patient_id BIGINT, task_id BIGINT,
  answers_json JSON, total_score INT,
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷答卷（P3b）';
