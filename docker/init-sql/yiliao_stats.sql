-- yiliao_stats 库（specs/modules/statistics.md §2；快照表为可再生预聚合数据）
-- 重建方式：statsAggregateJob 每轮「物理删旧 + 批量插新」同一事务（specs §4.1/§5），
-- 故 Mapper 侧使用物理 DELETE 而非 BaseEntity 的 @TableLogic 逻辑删除（逻辑删除会与唯一键冲突）；
-- deleted 列按全局表约定保留（specs/global/20 §1）。

USE yiliao_stats;

CREATE TABLE IF NOT EXISTS stats_overview_daily (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stat_date DATE NOT NULL COMMENT '统计日期',
  managing_count INT NOT NULL DEFAULT 0 COMMENT '在管患者数',
  active_plan_count INT NOT NULL DEFAULT 0 COMMENT '进行中随访计划数',
  overdue_task_count INT NOT NULL DEFAULT 0 COMMENT '逾期任务数',
  alert_count INT NOT NULL DEFAULT 0 COMMENT '预警数',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_stat_date(stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总览日快照';

CREATE TABLE IF NOT EXISTS stats_followup_monthly (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stat_month VARCHAR(7) NOT NULL COMMENT '统计月 yyyy-MM',
  due_count INT NOT NULL DEFAULT 0 COMMENT '应随访数(计划日期<=今日)',
  done_count INT NOT NULL DEFAULT 0 COMMENT '已完成数',
  timely_count INT NOT NULL DEFAULT 0 COMMENT '及时完成数(done_date<=plan_date)',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_stat_month(stat_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访完成率月快照';

CREATE TABLE IF NOT EXISTS stats_nodule_dist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dimension VARCHAR(32) NOT NULL COMMENT '维度:nodule_type/risk_level',
  label VARCHAR(32) NOT NULL COMMENT '标签:实性/部分实性/纯磨玻璃/低危/中危/高危',
  cnt INT NOT NULL DEFAULT 0 COMMENT '计数',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_dim_label(dimension, label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结节分布快照(每轮全量重建)';

CREATE TABLE IF NOT EXISTS stats_doctor_workload (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stat_date DATE NOT NULL COMMENT '统计日期',
  doctor_id BIGINT NOT NULL COMMENT '医生用户id',
  doctor_name VARCHAR(64) NOT NULL COMMENT '医生姓名(冗余展示,聚合维度非PII明细)',
  archive_count INT NOT NULL DEFAULT 0 COMMENT '建档数',
  done_count INT NOT NULL DEFAULT 0 COMMENT '完成任务数',
  alert_count INT NOT NULL DEFAULT 0 COMMENT '预警处理数',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_date_doctor(stat_date, doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生工作量日快照';
