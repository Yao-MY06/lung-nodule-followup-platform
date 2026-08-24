# R6：P1b 权限细化（菜单/用户 CRUD + 菜单树 + 管理员守卫）——唯一写代码任务

> 类型：代码任务（黄区）　前置：读 `specs/modules/auth.md`、`specs/global/10` §4-5、`AGENTS.md` 已知陷阱。

## 目标与边界

把"角色"从数据变成能力：菜单树返回、用户管理 CRUD、管理员接口守卫。

**范围（白名单）**：
- `yiliao-auth/**`（可改）：菜单/用户控制器、服务、菜单树组装
- `docker/init-sql/yiliao_auth.sql`：菜单种子数据追加（幂等）
- `yiliao-auth/src/test/`：新测试类
**禁区**：其他服务、common、api 契约模块、前端。守卫用**既有头部角色模式**（参照 `PatientRoleGuard`），不引入完整 Spring Security（一致性优先于框架纯度——毕设规模下 header 守卫已是项目既定模式，记录在实现说明）。

## 步骤

1. **菜单种子**（sys_menu/sys_role_menu，IF NOT EXISTS 幂等）：目录"系统管理"（菜单：用户管理、角色菜单只读展示），权限串示例 `auth:user:list`/`auth:user:write`；绑定到 ADMIN 角色。
2. **菜单树接口**：`GET /api/auth/menus`——按当前登录用户角色查 role_menu→menu，组树返回（menu_type 1 目录 2 菜单 3 按钮），患者角色返回空树（不报错）。
3. **用户管理 CRUD**（仅 ADMIN）：`GET /api/auth/users`（分页）/ `POST`（创建，密码 BCrypt、初始密码可配默认 `Init@123`）/ `PUT /{id}`（改 realName/phone/dept/status/角色）/ `PUT /{id}/password/reset`（重置密码，**改密后把该用户未过期 Token 拉黑——复用 TokenBlacklistService**）/ `DELETE /{id}`（逻辑删除）。
4. **守卫**：auth 模块新建 `AdminRoleGuard`（照 PatientRoleGuard 模式，header X-User-Roles 含 ADMIN 才放行，否则 403）；用户管理四个写接口 + 菜单管理写操作全部挂上。
5. **测试**（Mockito）：菜单树组装（父子/空角色）；非 ADMIN 调 users → 403；创建用户密码为 BCrypt 前缀 $2a$；改密后黑名单被调用（verify blacklistService.blacklist）。

## 验证表

| 检查 | 命令 | 通过标准 |
|---|---|---|
| 编译+全测 | `mvn install -pl yiliao-auth -f F:\bc\yiliao\pom.xml` | 全绿（含既有 12 用例回归） |
| 菜单树 | 登录 admin → GET /api/auth/menus | 返回目录树含系统管理/用户管理 |
| 越权 | doctor01 调 POST /api/auth/users | 403 |
| 密码安全 | 创建用户后查库 | password 为 $2a$ 前缀哈希，接口永不返回密码字段 |

## 已知陷阱

- 已有陷阱全适用（AGENTS.md）；新注意：menus 接口须走网关且非白名单——前端 P1b 菜单页不在本期范围。
- record 访问器、MP insert 重载 mock、Result.data()。

## 输出要求

摘要 → 变更文件清单 → 验证输出摘要 → 风险提醒（守卫为 header 模式而非 @PreAuthorize 的取舍理由写明）。
