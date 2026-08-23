# JMeter 压测脚本使用说明（P5）

> 配套方案：`../P5-熔断与压测方案.md` §2。脚本 `yiliao-login.jmx` 为 JMeter 5.6 标准格式（jmeterTestPlan version="1.2" properties="5.0"）。

## 环境要求

- JMeter 5.6+（`jmeter -v` 验证），JDK 17+。
- 被测环境：中间件 `docker compose up -d` + yiliao-auth（8081）+ yiliao-gateway（8080）已启动；压随访工作台还需 yiliao-followup（8084），压驾驶舱还需 yiliao-statistics（8087）。

## 脚本结构（默认值）

| 元素 | 默认值 | 说明 |
|---|---|---|
| 线程组「登录压测」 | 线程数 10，Ramp-up 10s | 无限循环 + 调度器持续时间 60s（`continue_forever=true` + `duration=60`） |
| HTTP 请求 | `POST http://localhost:8080/api/auth/login` | Body：`{"username":"admin","password":"admin123"}`（种子开发账号） |
| 信息头管理器 | `Content-Type: application/json` | 压鉴权接口时在此追加 Authorization（见下） |
| 监听器 | Summary 聚合报告 + View Results Tree | 命令行模式以 `-e -o` 的 HTML 报告为准 |

## 快速开始（命令行）

```bash
cd tasks/jmeter
# 10 并发档（-o 目录必须不存在或为空；-l 文件不可复用）
jmeter -n -t yiliao-login.jmx -l result-login-10.jtl -e -o report-login-10
```

跑完打开 `report-login-10/index.html` → Statistics 表，记录 Average、90th/95th pct、Throughput、Error%。

## 三档并发对比（10 / 50 / 100）

只需改一处：`ThreadGroup.num_threads`。

- 文本编辑器：直接编辑 `yiliao-login.jmx`，把
  `<stringProp name="ThreadGroup.num_threads">10</stringProp>` 改为 `50`（或 `100`）；
- 或 JMeter GUI：打开脚本 → 线程组「登录压测」→ 线程数改 50 → 另存为 `yiliao-login-50.jmx`。

每档换输出名执行：

```bash
jmeter -n -t yiliao-login.jmx       -l result-login-10.jtl  -e -o report-login-10
jmeter -n -t yiliao-login-50.jmx    -l result-login-50.jtl  -e -o report-login-50
jmeter -n -t yiliao-login-100.jmx   -l result-login-100.jtl -e -o report-login-100
```

时长/Ramp-up 如需调整：`ThreadGroup.duration`（默认 60）与 `ThreadGroup.ramp_time`（默认 10）。

## 切换目标接口（工作台 / 驾驶舱）

这两个接口需要 Token，步骤：

1. 登录取 accessToken：
   ```bash
   curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   ```
   （access Token 有效期 2h，60s 一轮压测单 Token 足够。）
2. 复制脚本为 `yiliao-workbench.jmx` / `yiliao-stats.jmx`，改三处：
   - `HTTPSampler.path`：`/api/auth/login` → `/api/followup/workbench?status=1&page=1&size=10` 或 `/api/stats/overview`；
   - `HTTPSampler.method`：`POST` → `GET`；
   - 删除 JSON body：整个 `<elementProp name="" elementType="HTTPArgument">...</elementProp>` 节点；
3. 在 HeaderManager 的 `<collectionProp name="HeaderManager.headers">` 内追加一行请求头：
   ```xml
   <elementProp name="" elementType="Header">
     <stringProp name="Header.name">Authorization</stringProp>
     <stringProp name="Header.value">Bearer {accessToken}</stringProp>
   </elementProp>
   ```

## 注意事项

- **压测前确认 Sentinel 流控规则已删除或阈值调大**，否则限流会干扰 RT/错误率统计（正在演示限流时除外）。
- 驾驶舱对比实验（P5b）：加缓存前后各压一轮同档位（建议 50 并发），输出目录分别命名 `report-overview-before/`、`report-overview-after/`，截图 Statistics 表做论文对比图。
- 压测机与被测服务同机，论文口径为"单机相对对比"，不宣称绝对容量。
- 不要用 GUI 模式跑正式压测（GUI 监听器会影响结果），GUI 只用于改脚本和调试。
