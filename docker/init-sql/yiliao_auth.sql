-- yiliao_auth 库：DDL + 种子数据（specs/modules/auth.md §2，完整 DDL 定义见 make/技术设计文档 §6）
-- 幂等：表用 IF NOT EXISTS；种子用 NOT EXISTS 防重复插入

USE yiliao_auth;

CREATE TABLE IF NOT EXISTS sys_user (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  username      VARCHAR(32)  NOT NULL UNIQUE COMMENT '登录名',
  password      VARCHAR(100) NOT NULL COMMENT 'BCrypt加密',
  real_name     VARCHAR(32)  NOT NULL,
  phone         VARCHAR(20),
  user_type     TINYINT NOT NULL COMMENT '1管理员 2医生 3随访专员 4患者',
  dept          VARCHAR(64),
  status        TINYINT DEFAULT 1 COMMENT '1正常 0停用',
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(32) NOT NULL UNIQUE COMMENT 'ADMIN/DOCTOR/NURSE/PATIENT',
  role_name VARCHAR(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL, role_id BIGINT NOT NULL,
  PRIMARY KEY(user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0,
  menu_name VARCHAR(64), path VARCHAR(128),
  perms VARCHAR(64) COMMENT '如 followup:task:list',
  menu_type TINYINT COMMENT '1目录 2菜单 3按钮',
  sort INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/按钮权限';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id BIGINT NOT NULL, menu_id BIGINT NOT NULL,
  PRIMARY KEY(role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单';

-- ── 种子数据 ─────────────────────────────────────────────
INSERT INTO sys_role (id, role_code, role_name)
SELECT 1, 'ADMIN', '系统管理员' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE id = 1);
INSERT INTO sys_role (id, role_code, role_name)
SELECT 2, 'DOCTOR', '医生' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE id = 2);
INSERT INTO sys_role (id, role_code, role_name)
SELECT 3, 'NURSE', '随访专员' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE id = 3);
INSERT INTO sys_role (id, role_code, role_name)
SELECT 4, 'PATIENT', '患者' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE id = 4);

-- 开发账号：admin/admin123、doctor01/doctor123（密码 BCrypt，仅本地开发环境）
INSERT INTO sys_user (id, username, password, real_name, user_type, dept, status)
SELECT 1, 'admin', '$2a$10$qHjPJEVImoIoVMzFD2cRfOvM2jVqBi4Zu3bc1lfpgXq560yxbfLfO',
       '系统管理员', 1, '信息科', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 1);
INSERT INTO sys_user (id, username, password, real_name, user_type, dept, status)
SELECT 2, 'doctor01', '$2a$10$7TKk0QWxR1NqzesN7mu1AO3.LOQANdJTCaNCcBu9WleMRFH2nlr2m',
       '示例医生', 2, '呼吸科', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 2);

INSERT INTO sys_user_role (user_id, role_id)
SELECT 1, 1 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 1 AND role_id = 1);
INSERT INTO sys_user_role (user_id, role_id)
SELECT 2, 2 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 2 AND role_id = 2);
