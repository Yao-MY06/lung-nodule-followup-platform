# 认证中心 auth（yiliao-auth / 8081）

> 文档类型：业务模块文档　所属模块：auth　依赖文档：[global/10, global/20, global/30]
> 版本：v1.0（2026-08-22）　阅读优先级：高　风险车道：**红区**

## 1. 职责边界与能力清单

- 登录认证、Token 签发/刷新/注销（JWT + Redis 黑名单，规范见【global/10 §5】）。
- 用户/角色/菜单 RBAC 管理；医生-患者绑定关系维护（主管医生负责制）。
- **不做**：患者临床档案（归 patient-service）、消息推送。

## 2. 数据模型

库 `yiliao_auth`，完整 DDL 见 make/技术设计文档 §6。

| 表 | 关键点 |
|---|---|
| sys_user | `password` BCrypt；`user_type` 见 UserType 枚举；患者账号 `user_id` 被 patient_archive 引用 |
| sys_role / sys_user_role | 角色码 ADMIN/DOCTOR/NURSE/PATIENT |
| sys_menu / sys_role_menu | `perms` 格式 `服务:资源:操作`（如 `followup:task:list`），树形 parent_id |

- 初始种子：admin 账号 + 4 角色 + 菜单树（docker/init-sql/yiliao_auth.sql）。

## 3. 接口定义

REST（/api/auth）：`POST /login`、`POST /refresh`、`POST /logout`、`GET /menus`、`GET|POST|PUT|DELETE /users`（管理员）。

Feign 契约 `AuthApi`：`getUserInfo(id)`、`batchUserInfos(ids)`（返回 id/realName/userType，供各服务显示操作者姓名）。

## 4. 业务规则

1. 登录失败统一提示 `10001 用户名或密码错误`（不区分账号不存在/密码错，防枚举）；连续失败 5 次锁定 10 分钟（Redis 计数）。
2. Token：access 2h / refresh 7d；`jti` 唯一；注销与修改密码将未过期 access Token 写入黑名单。
3. 密码策略：BCrypt 强度 10；毕设不强制复杂度轮换，但禁止明文存储/传输/打日志。
4. 菜单接口按当前用户角色过滤返回权限树。

## 5. 异常场景

- Redis 不可用：登录降级为"仅本地 JWT 签发"并告警（黑名单失效窗口内风险可接受，记录取舍）；refresh 不可用。
- 用户被停用（status=0）：已签发 Token 下次请求由网关查黑名单+auth 缓存拦截（用户状态缓存 5min）。

## 6. 禁止行为

1. 禁止任何接口返回密码字段（含哈希）。
2. 禁止非管理员接口修改角色/菜单；权限校验必须服务端 `@PreAuthorize`，不信任前端菜单隐藏。
3. 禁止将 JWT 密钥硬编码（Nacos 配置）。

## 7. 依赖声明

- 公共能力：common-security、common-web、common-data。
- 下游：被 gateway 调用（登录/刷新）；被各服务经 `AuthApi` 查用户信息。
- 中间件：MySQL(yiliao_auth)、Redis。

## 8. 最小调用示例

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"doctor01","password":"******"}'
# → {"code":0,"msg":"ok","data":{"accessToken":"...","refreshToken":"...","userType":2}}
```

## 变更记录

| 版本 | 日期 | 变更 | 原因 |
|---|---|---|---|
| v1.0 | 2026-08-22 | 首版，收敛自 make/技术设计文档 §6/§12/§22 | 分层文档体系建立 |
