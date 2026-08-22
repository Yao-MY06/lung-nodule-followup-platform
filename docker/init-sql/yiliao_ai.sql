-- yiliao_ai 库（specs/modules/ai.md §2；DDL 源自 make/技术设计文档 §11）
-- 向量切片 P4b 迁 PgVector，本期 chunk 在服务内存库（重启重导入，演示足够）

USE yiliao_ai;

CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT,
  doctor_id BIGINT,
  title VARCHAR(128),
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 会话（P4b 落库）';

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(16) COMMENT 'user/assistant/tool',
  content TEXT,
  tool_calls_json JSON COMMENT '工具调用审计',
  citations_json JSON COMMENT 'RAG引用出处',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_session(session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 消息（P4b 落库）';

CREATE TABLE IF NOT EXISTS knowledge_doc (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_name VARCHAR(128) COMMENT '如:肺结节诊治中国专家共识2024',
  version VARCHAR(32),
  chunk_count INT,
  status TINYINT COMMENT '1已入库',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指南文档登记';
