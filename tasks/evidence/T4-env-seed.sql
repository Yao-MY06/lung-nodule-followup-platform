-- T4 环境重建：对历史数据卷（yiliao_mysql-data，建库于旧版 init-sql）执行的最小结构补齐
-- 背景：docker/init-sql/yiliao_patient.sql 现行 DDL 含 id_card_hash（PII 哈希列，specs PII 加固新增），
--       历史卷建库时该列尚不存在，导致 patient 服务查询报 Unknown column 'id_card_hash'（HTTP 500）。
--       经 information_schema 全量 diff（yiliao_auth/patient/nodule/followup 四库 203 列），唯一漂移即此列。
-- 数据说明：仅结构补齐，无 INSERT/UPDATE；patient01(id=3)/档案1001/随访计划(id=3)/任务(5条) 均为历史卷原生数据。
-- 本文件仅本地使用，不含任何密码。

USE yiliao_patient;

-- 对照 init-sql：id_card_hash CHAR(64) COMMENT '身份证规范化值 SHA-256，不可逆'；UNIQUE KEY uk_id_card_hash(id_card_hash)
-- 历史行该列为 NULL（MySQL 唯一索引允许多个 NULL），不加 NOT NULL 以兼容存量数据
ALTER TABLE patient_archive
  ADD COLUMN id_card_hash CHAR(64) NULL AFTER id_card,
  ADD UNIQUE KEY uk_id_card_hash (id_card_hash);
