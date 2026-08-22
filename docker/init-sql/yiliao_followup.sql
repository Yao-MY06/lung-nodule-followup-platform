-- yiliao_followup 库（specs/modules/followup.md §2；DDL 源自 make/技术设计文档 §9，补 create_by/update_by）
-- cond_json 条件语法（specs/modules/followup.md §4）：
--   {"nodule_type":3, "max_dia":"<=5", "risk":false, "stage_group":"II_III", "gene_positive":true, "adjuvant":true}
--   数值运算符："<=x" ">=" "<x" ">x" "a-b"(闭区间) 或纯数字(相等)；布尔/枚举直接等值；缺省键=不限制

USE yiliao_followup;

CREATE TABLE IF NOT EXISTS followup_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_name VARCHAR(64) NOT NULL COMMENT '如:pGGN≤5mm年度随访',
  scene TINYINT COMMENT '1结节随访 2术后随访 3治疗期随访',
  guideline_source VARCHAR(128) COMMENT '指南出处',
  items_json JSON COMMENT '每次随访项目清单',
  status TINYINT DEFAULT 1,
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访方案模板';

CREATE TABLE IF NOT EXISTS decision_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene TINYINT COMMENT '同template.scene',
  cond_json JSON NOT NULL COMMENT '条件(见文件头注释)',
  result_template_id BIGINT NOT NULL,
  first_interval_month INT COMMENT '首次随访间隔(月)',
  repeat_interval_month INT,
  total_years INT COMMENT '总随访年限',
  priority INT DEFAULT 0 COMMENT '多规则命中取最高',
  status TINYINT DEFAULT 1,
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_scene(scene, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='决策规则(表驱动引擎)';

CREATE TABLE IF NOT EXISTS followup_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  rule_id BIGINT,
  start_date DATE NOT NULL,
  end_date DATE,
  status TINYINT COMMENT '1进行中 2已完成 3已终止',
  active_flag TINYINT GENERATED ALWAYS AS (CASE WHEN status = 1 THEN 1 ELSE NULL END) STORED
    COMMENT '仅进行中计划为1，配合唯一键保证每患者单一进行中计划',
  adjust_reason VARCHAR(255) COMMENT '人工调整/升级说明',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_patient_active(patient_id, active_flag),
  KEY idx_patient(patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访计划';

CREATE TABLE IF NOT EXISTS followup_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  seq INT COMMENT '第N次随访',
  plan_date DATE NOT NULL,
  items_json JSON COMMENT '本次随访项目',
  status TINYINT DEFAULT 0 COMMENT '0未到期 1临期 2逾期 3已完成 4失访 5已取消',
  remind_sent TINYINT DEFAULT 0 COMMENT '已发提醒次数',
  done_date DATE,
  operator_id BIGINT,
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_plan(plan_id), KEY idx_patient(patient_id),
  KEY idx_scan(status, plan_date) COMMENT 'XXL-Job扫描索引(P3)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单次随访任务(高频表)';

CREATE TABLE IF NOT EXISTS followup_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  followup_type TINYINT COMMENT '1电话 2门诊 3线上问卷',
  content TEXT COMMENT '随访内容/话术记录',
  result_summary VARCHAR(500),
  next_advice VARCHAR(255),
  operator_id BIGINT,
  followup_time DATETIME,
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_task(task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访执行记录';

CREATE TABLE IF NOT EXISTS symptom_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  symptom VARCHAR(64) COMMENT '疼痛/咳嗽/乏力…',
  severity TINYINT COMMENT '1-5级,>=3预警',
  description VARCHAR(500),
  source TINYINT COMMENT '1患者端 2AI助手',
  alert_flag TINYINT DEFAULT 0 COMMENT '是否触发预警',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_patient(patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ePRO症状上报';

-- ══ 种子：随访模板（需求分析报告 §3.2/§3.3 指南规则固化）══════════════
INSERT INTO followup_template (id, template_name, scene, guideline_source, items_json)
SELECT 1, '年度LDCT随访', 1, '肺结节诊治中国专家共识(2024)', JSON_OBJECT('items', JSON_ARRAY('胸部LDCT'))
WHERE NOT EXISTS (SELECT 1 FROM followup_template WHERE id = 1);
INSERT INTO followup_template (id, template_name, scene, guideline_source, items_json)
SELECT 2, '半年CT随访', 1, '肺结节诊治中国专家共识(2024)', JSON_OBJECT('items', JSON_ARRAY('胸部CT'))
WHERE NOT EXISTS (SELECT 1 FROM followup_template WHERE id = 2);
INSERT INTO followup_template (id, template_name, scene, guideline_source, items_json)
SELECT 3, '季度CT密切随访', 1, '肺结节诊治中国专家共识(2024)', JSON_OBJECT('items', JSON_ARRAY('胸部CT'))
WHERE NOT EXISTS (SELECT 1 FROM followup_template WHERE id = 3);
INSERT INTO followup_template (id, template_name, scene, guideline_source, items_json)
SELECT 4, '术后随访-局部方案A', 2, '国家卫健委肺癌诊疗指南/胸外科专家共识',
       JSON_OBJECT('items', JSON_ARRAY('病史与查体','肿瘤标志物','胸部CT'))
WHERE NOT EXISTS (SELECT 1 FROM followup_template WHERE id = 4);
INSERT INTO followup_template (id, template_name, scene, guideline_source, items_json)
SELECT 5, '术后随访-方案A/B交替', 2, '国家卫健委肺癌诊疗指南/胸外科专家共识',
       JSON_OBJECT('items', JSON_ARRAY('病史与查体','肿瘤标志物','胸部CT','腹部影像','头颅增强MRI','骨扫描'))
WHERE NOT EXISTS (SELECT 1 FROM followup_template WHERE id = 5);
INSERT INTO followup_template (id, template_name, scene, guideline_source, items_json)
SELECT 6, '小细胞肺癌全面随访', 2, '国家卫健委肺癌诊疗指南',
       JSON_OBJECT('items', JSON_ARRAY('胸腹盆增强CT','头颅MRI','骨扫描','肿瘤标志物','戒烟评估'))
WHERE NOT EXISTS (SELECT 1 FROM followup_template WHERE id = 6);

-- ══ 种子：决策规则（结节随访 scene=1；priority 越大越特异）════════════
-- 实性结节 ≤8mm 无危险因素（§3.2 表一）
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 1, 1, JSON_OBJECT('nodule_type',1,'max_dia','<=4','risk',false), 1, 12, 12, 5, 20
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 1);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 2, 1, JSON_OBJECT('nodule_type',1,'max_dia','4-6','risk',false), 1, 12, 12, 5, 20
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 2);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 3, 1, JSON_OBJECT('nodule_type',1,'max_dia','6-8','risk',false), 2, 9, 12, 5, 20
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 3);
-- 实性结节 ≤8mm 有危险因素（§3.2 表二；间隔取指南区间中值）
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 4, 1, JSON_OBJECT('nodule_type',1,'max_dia','<=4','risk',true), 1, 12, 12, 5, 30
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 4);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 5, 1, JSON_OBJECT('nodule_type',1,'max_dia','4-6','risk',true), 2, 9, 12, 5, 30
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 5);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 6, 1, JSON_OBJECT('nodule_type',1,'max_dia','6-8','risk',true), 3, 6, 6, 5, 30
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 6);
-- 磨玻璃/部分实性（§3.2 表三）
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 7, 1, JSON_OBJECT('nodule_type',3,'max_dia','<=5'), 2, 6, 12, 5, 40
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 7);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 8, 1, JSON_OBJECT('nodule_type',3,'max_dia','>5'), 3, 3, 12, 5, 40
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 8);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 9, 1, JSON_OBJECT('nodule_type',2,'max_dia','<=8'), 3, 3, 6, 3, 40
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 9);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 10, 1, JSON_OBJECT('nodule_type',2,'max_dia','>8'), 3, 3, 3, 5, 40
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 10);

-- ══ 种子：决策规则（术后随访 scene=2，§3.3 表）════════════════════════
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 11, 2, JSON_OBJECT('stage_group','CIS'), 1, 12, 12, 10, 50
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 11);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 12, 2, JSON_OBJECT('stage_group','I'), 4, 6, 6, 5, 50
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 12);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 13, 2, JSON_OBJECT('stage_group','II_III'), 5, 3, 3, 5, 50
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 13);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 14, 2, JSON_OBJECT('stage_group','EGFR_ADJ'), 5, 3, 6, 6, 60
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 14);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 15, 2, JSON_OBJECT('stage_group','IV'), 5, 2, 2, 3, 50
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 15);
INSERT INTO decision_rule (id, scene, cond_json, result_template_id, first_interval_month, repeat_interval_month, total_years, priority)
SELECT 16, 2, JSON_OBJECT('stage_group','SCLC_LIMITED'), 6, 3, 3, 5, 50
WHERE NOT EXISTS (SELECT 1 FROM decision_rule WHERE id = 16);
