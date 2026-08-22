-- yiliao_nodule 库（specs/modules/nodule.md §2；DDL 源自 make/技术设计文档 §8，补 create_by/update_by）

USE yiliao_nodule;

CREATE TABLE IF NOT EXISTS nodule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  nodule_no  VARCHAR(32) COMMENT '患者内结节编号N1/N2…',
  location   VARCHAR(64) COMMENT '右肺上叶尖段…',
  nodule_type TINYINT NOT NULL COMMENT '1实性 2部分实性 3纯磨玻璃',
  multiplicity TINYINT COMMENT '1单发 2多发',
  status     TINYINT COMMENT '1随访中 2已手术 3排除',
  first_found_date DATE,
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_patient(patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结节';

CREATE TABLE IF NOT EXISTS nodule_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nodule_id  BIGINT NOT NULL,
  report_id  BIGINT COMMENT '关联exam_report',
  exam_date  DATE NOT NULL,
  max_diameter_mm DECIMAL(5,1) COMMENT '最大径',
  solid_diameter_mm DECIMAL(5,1) COMMENT '实性成分径',
  mean_density_hu INT COMMENT '平均CT值',
  signs VARCHAR(255) COMMENT '恶性征象JSON:分叶/毛刺/胸膜牵拉…',
  volume_mm3 DECIMAL(10,1) COMMENT '体积(AI可选)',
  is_new TINYINT DEFAULT 0 COMMENT '是否新发',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_nodule_date(nodule_id, exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复查快照(纵向对比)';

CREATE TABLE IF NOT EXISTS exam_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  report_type TINYINT COMMENT '1CT 2PET-CT 3MRI 4肿瘤标志物 5病理 6基因',
  exam_date DATE NOT NULL,
  org_name VARCHAR(64),
  raw_text TEXT COMMENT '报告原文(AI抽取输入,P4)',
  structured_json JSON COMMENT 'AI抽取结构化结果',
  conclusion VARCHAR(500),
  file_url VARCHAR(255) COMMENT 'MinIO附件',
  extract_status TINYINT COMMENT '0未抽取 1已抽取 2已确认',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_patient_date(patient_id, exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查报告';

CREATE TABLE IF NOT EXISTS lab_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL, exam_date DATE NOT NULL,
  item_code VARCHAR(16) COMMENT 'CEA/CYFRA21-1/NSE/SCC/CA125',
  item_value DECIMAL(12,3), unit VARCHAR(16),
  abnormal_flag TINYINT COMMENT '0正常 1偏高 2偏低',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_patient_item(patient_id, item_code, exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='肿瘤标志物(P2 仅建表)';
