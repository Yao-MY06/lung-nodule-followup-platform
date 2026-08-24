# R5：JMeter 压测基线 + Sentinel 限流演示

> 类型：运行态联调　前置：服务链运行（gateway+auth+followup+statistics+patient）；JMeter 已安装（5.6+，JAVA_HOME 指向 JDK）。

## 目标

1. 三个接口基线压测出图（论文"性能功底"素材）：登录、工作台、驾驶舱
2. Sentinel 网关限流演示（11001 统一响应）

## 步骤

### S1 基线压测

用 `tasks/jmeter/yiliao-login.jmx` 为模板（README 里有改法）复制三份场景。每场景三档：10/50/100 并发 × 60s，Ramp-up 10s。

```bash
jmeter -n -t tasks/jmeter/yiliao-login.jmx -l tasks/evidence/R5-login-10u.jtl -e -o tasks/evidence/R5-report-login-10u
# 重复 50/100 并发与 followup-workbench、stats-overview（Token 头用 admin 的 accessToken）
```

接口清单（经网关 8080）：
- POST /api/auth/login（{"username":"admin","password":"admin123"}，无 Token）
- GET /api/followup/workbench?status=1&page=1&size=10（Header: Authorization Bearer {token}）
- GET /api/stats/overview（同上）

出表（Markdown 存 tasks/evidence/R5-baseline-table.md）：每接口每档的 平均RT/P95/QPS/错误率。QPS<并发数下限的档位标注"达到服务端瓶颈"并给出 RT 拐点分析。

### S2 Sentinel 限流演示

1. compose 注释区启用 sentinel-dashboard（8858），gateway 取消注释 sentinel 配置后重启：
   ```bash
   mvn -q -f F:\bc\yiliao\pom.xml -pl yiliao-gateway spring-boot:run
   ```
2. 控制台 → 资源 `/api/auth/login` 配流控规则 QPS=10。
3. JMeter 50 并发打 login 30s → 预期：部分请求 HTTP 429 + body `{"code":11001,"msg":"请求过于频繁，请稍后再试"}`（SentinelBlockHandlerConfig 统一响应）。
4. 截图：控制台实时监控被限流曲线 + JMeter 聚合报告错误率跳变 → tasks/evidence/R5-sentinel-*.png。

### S3（可选 P5b 钩子）缓存对比

两级缓存未实装（P5b）——本版只出基线图；文档表格留一行"P5b 缓存接入后重测对比"占位，注明不虚构对比数字。

## 验证表

| 检查 | 证据 | 通过标准 |
|---|---|---|
| 基线九组 | 9 个 .jtl + HTML 报告 | RT/QPS 表完整，错误率=0%（无规则时） |
| 限流 | 429+11001 截图 | 规则生效且响应为统一 JSON |
| 恢复 | 删规则后重跑 10u | 错误率回到 0% |

## 禁止

不修改 JMeter 之外的任何源码；不在报告里虚构缓存对比数据。
