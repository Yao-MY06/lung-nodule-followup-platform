-- yiliao_patient 库（specs/modules/patient.md §2；DDL 源自 make/技术设计文档 §7，补 create_by/update_by 对齐 G20 §1）

USE yiliao_patient;

CREATE TABLE IF NOT EXISTS patient_archive (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_no    VARCHAR(32)  NOT NULL UNIQUE COMMENT '档案编号',
  user_id       BIGINT COMMENT '关联sys_user(患者账号)',
  name          VARCHAR(32)  NOT NULL,
  gender        TINYINT COMMENT '1男 2女',
  birth_date    DATE,
  id_card       VARCHAR(128) COMMENT '加密存储/展示脱敏',
  id_card_hash  CHAR(64) COMMENT '身份证规范化值 SHA-256，不可逆',
  phone         VARCHAR(20) COMMENT '展示脱敏',
  address       VARCHAR(255),
  emergency_contact VARCHAR(32),
  emergency_phone   VARCHAR(20),
  doctor_id     BIGINT COMMENT '主管医生',
  stage_label   VARCHAR(32) COMMENT '结节随访中/术后随访中/治疗中/MDT中/失访/结案',
  source_type   TINYINT COMMENT '1体检 2门诊 3住院',
  remark        VARCHAR(500),
  create_by     BIGINT, update_by BIGINT,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT DEFAULT 0,
  UNIQUE KEY uk_id_card_hash(id_card_hash),
  KEY idx_doctor(doctor_id), KEY idx_stage(stage_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者档案';

CREATE TABLE IF NOT EXISTS patient_risk_factor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  smoking_pack_year DECIMAL(6,1) COMMENT '吸烟包年',
  family_history  TINYINT COMMENT '0无 1有',
  occupational_exposure TINYINT,
  prior_cancer    TINYINT,
  comorbidity     VARCHAR(255) COMMENT '合并症(COPD等)',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_patient(patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危险因素(影响随访策略)';

CREATE TABLE IF NOT EXISTS patient_diagnosis (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  tnm_stage    VARCHAR(16) COMMENT '如 T1bN0M0',
  clinical_stage VARCHAR(8) COMMENT 'IA2/IIIA…',
  pathology_type VARCHAR(32),
  gene_result  VARCHAR(255) COMMENT 'EGFR/ALK/ROS1等',
  surgery_date DATE,
  adjuvant_therapy TINYINT,
  diagnose_date DATE,
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_patient(patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='肺癌诊断(可多条)';
