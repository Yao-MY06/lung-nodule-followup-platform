-- P0：仅建库。业务表 DDL 随阶段进入对应库文件（auth → P1，patient/nodule/followup → P2，notify → P3，ai → P4，stats → P5）
-- 库归属见 specs/global/30 §4

CREATE DATABASE IF NOT EXISTS yiliao_auth     DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS yiliao_patient  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS yiliao_nodule   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS yiliao_followup DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS yiliao_notify   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS yiliao_ai       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS yiliao_stats    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- P3 时启用：CREATE DATABASE IF NOT EXISTS xxl_job ...（导入 xxl-job 官方 tables_xxl_job.sql）
