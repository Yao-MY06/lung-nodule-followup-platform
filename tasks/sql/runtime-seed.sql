-- 运行时验证种子（不进 init-sql，只服务演示；越权防线实测用）
-- patient01 密码复用 admin123 的 BCrypt 哈希（演示环境专用，报告注明）

USE yiliao_auth;
INSERT INTO sys_user (id, username, password, real_name, user_type, dept, status)
SELECT 3, 'patient01', '$2a$10$qHjPJEVImoIoVMzFD2cRfOvM2jVqBi4Zu3bc1lfpgXq560yxbfLfO', '演示患者', 4, NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 3);
INSERT INTO sys_user_role (user_id, role_id)
SELECT 3, 4 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 3 AND role_id = 4);

-- 演示患者档案：archive id=1001 绑定 user_id=3（patient01）
USE yiliao_patient;
INSERT INTO patient_archive (id, patient_no, user_id, name, gender, birth_date, phone, doctor_id, stage_label, source_type)
SELECT 1001, 'P20260823-1001', 3, '演示患者', 1, '1965-03-12', '13812345678', 2, '结节随访中', 1
WHERE NOT EXISTS (SELECT 1 FROM patient_archive WHERE id = 1001);
INSERT INTO patient_risk_factor (patient_id, smoking_pack_year, family_history, occupational_exposure, prior_cancer)
SELECT 1001, 30, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM patient_risk_factor WHERE patient_id = 1001);

-- 对照组：他人档案 id=1002（患者01 越权访问的目标）
INSERT INTO patient_archive (id, patient_no, user_id, name, gender, stage_label, source_type)
SELECT 1002, 'P20260823-1002', NULL, '对照患者', 2, '结节随访中', 2
WHERE NOT EXISTS (SELECT 1 FROM patient_archive WHERE id = 1002);
