# 网关 gateway（yiliao-gateway / 8080）

> 文档类型：业务模块文档　所属模块：gateway　依赖文档：[global/10, global/20, global/30]
> 版本：v1.0（2026-08-22）　阅读优先级：高　风险车道：**红区**（鉴权入口）

## 1. 职责边界与能力清单

- 统一入口：路由转发、JWT 校验、Redis 黑名单拦截、限流、CORS、聚合 Knife4j 文档。
- 校验通过后透传 `X-User-Id / X-User-Roles / X-Trace-Id` 内部头（详见【global/10 §5】）。
- **不做**：任何业务逻辑、不解析请求体、不直连业务库。

## 2. 数据模型

无业务表；仅依赖 Redis（Token 黑名单、限流计数）。路由配置存 Nacos `yiliao-gateway.yaml`，动态刷新。

## 3. 接口定义

无自有 REST 接口。路由表（Nacos 维护）：

| 路径前缀 | 目标服务 |
|---|---|
| /api/auth/** | yiliao-auth |
| /api/patient/** | yiliao-patient |
| /api/nodule/**, /api/exam/** | yiliao-nodule |
| /api/followup/** | yiliao-followup |
| /api/notify/** | yiliao-notification |
| /api/ai/** | yiliao-ai |
| /api/stats/** | yiliao-statistics |
| /api/file/** | yiliao-file |

- 白名单（免鉴权）：`/api/auth/login`、`/api/auth/refresh`、Knife4j 资源。

## 4. 业务规则

1. JWT 校验失败/过期/在黑名单 → `401`；角色不匹配 → `403`（错误体遵循【global/10 §2】）。
2. 限流（Sentinel）：全局 QPS 阈值 + 按用户维度（`X-User-Id`）阈值；超限返回 `11001 触发限流`。
3. `/api/portal/**`（患者端）要求角色含 PATIENT，其余请求仅要求有效 Token，权限由各服务 `@PreAuthorize` 细化。

## 5. 异常场景

- 下游服务不可达：网关返回 `503` + 服务名，前端提示"服务暂时不可用"；不重试非幂等请求。
- Redis 不可用：**fail-open 放行请求并告警**（黑名单检查降级，取舍：可用性优先，因黑名单是少数场景），限流降级为仅全局限流。

## 6. 禁止行为

1. 禁止在网关写业务逻辑或聚合业务数据。
2. 禁止在网关之外校验/解析 JWT 密钥之外的任何 Token 细节。
3. 禁止将内部头（X-User-*）转发到外部网络出口（仅服务间）。

## 7. 依赖声明

- 公共能力：`common-security`（JWT 解析）、Redis、Sentinel。
- 上游：唯一流量入口；下游：全部 8 个业务服务。

## 8. 最小调用示例

```bash
# 携带 Token 访问随访工作台
curl -H "Authorization: Bearer {accessToken}" \
     http://localhost:8080/api/followup/workbench?status=DUE_SOON&page=1&size=10
```

## 变更记录

| 版本 | 日期 | 变更 | 原因 |
|---|---|---|---|
| v1.0 | 2026-08-22 | 首版，收敛自 make/技术设计文档 §4 | 分层文档体系建立 |
