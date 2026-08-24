# P0~P5 外部模型重放 · 完成报告（评审素材 + 偏差披露）

> 日期：2026-08-24 · 重放目录：`F:\bc\yiliao-p0-replay`（git 六提交）· 比对基线：`F:\bc\yiliao` 主仓
> 用途：供主线按 README 五维度差异评审（版本一致性/签名一致性/依赖方向/验证命令/逐字比对）做论文"AI 辅助开发方法论"实验数据。**§4 偏差披露为必读，部分维度对部分阶段无效，先读再做评审。**

## 0. 结论（TLDR）

- 六段执行书依次投喂外部模型在空目录串行重放，**18 模块全 BUILD SUCCESS、115 测试全绿**，主线对每阶段独立复验（不信任子代理自报）。
- **纯度问题（最重要）**：kimi 供应商自 P2 末起持续不可用（累计 7 次瞬时鉴权失败，均 1.2~1.5s、无部分产出）。纯 kimi 输出仅 P0、P1 两段；P2 为 kimi 主体 + 通用子代理收尾（其中 3 份 DDL 与主仓字节级相同，来源存疑）；**P3/P4/P5 全程为通用子代理（非 kimi）输出**。若实验设计锁定"单一外部模型"，P3~P5 需在 kimi 恢复后重放（从 P2 提交 `0aba0c9` 起重跑即可）。
- 逐字锁定值（版本/DDL/yml/正则/Prompt/错误码/bizKey）命中率极高；实质差异集中在少数可归因项（§3.2），无一处违反医疗安全不变量。
- 发现**执行书缺陷 2 处**（compatibility-verifier 启动阻塞未覆盖、P0 测试清单缺 11 个 common 测试）与**主仓遗留 1 处**（auth yml 无效 Redisson exclude）。

## 1. 重放总览

| 阶段 | 提交 | 提交时间 | 执行模型 | 新增测试 | 主线独立验证 |
|---|---|---|---|---|---|
| P0 工程底座 | `1181c50` | 08-24 11:21 | kimi | 0 | 9 模块 BUILD SUCCESS |
| P1 接入层 | `79195b9` | 08-24 11:48 | kimi | 19（JWT 7+auth 5+gateway 7） | 11 模块、19 测试全绿 |
| P2 核心域 | `0aba0c9` | 08-24 12:40 | kimi 主体（31min 后 provider 中断）+ 通用子代理收尾 | 35（patient 6+nodule 6+followup 23） | 14 模块、54 测试全绿 |
| P3 消息闭环 | `ac0d1de` | 08-24 13:18 | **通用子代理全程**（kimi 3 次失败；耗时 14.9min） | 17（notification 6+followup 11） | 15 模块、71 测试全绿 |
| P4 AI 能力 | `d89423c` | 08-24 13:46 | **通用子代理全程**（kimi 2 次失败；耗时 13.5min） | 24（ai 20+nodule 4） | 16 模块、95 测试全绿 |
| P5 呈现治理 | `e2aebe0` | 08-24 14:32 | **通用子代理全程**（kimi 2 次失败；耗时 32.7min） | 20（statistics 14+file 6） | 18 模块、115 测试全绿 |

kimi 失败记录（供应商侧，均 "Provider authentication failed"，无产出、工作树保持干净）：P3×3（1.35s/1.31s/1.20s）、P4×2（1.42s/1.32s）、P5×2（1.46s/1.36s）。

## 2. 最终验证结果（主线独立执行，非子代理自报）

- `mvn install`（JDK 25 / Maven 3.9.16）：**18/18 模块 BUILD SUCCESS**，测试 115 = 7+7+5+6+10+34+6+20+14+6，Failures 0 / Errors 0。
- 逐字抽查命中：bizKey `{taskId}:REMIND_T{7|3|1}`（生产端 switch 逐字）；站内信文案/三档标题；脱敏 5 条正则（顺序正确）；两处 SSE 免责语常量；4 个 @Tool 方法签名无 patientId；file 白名单/前缀/100MB/600s/1800s/对象键正则。
- 负面 grep 全零：`setDelayTimeLevel` 0、`ShardingUtil.` 0、notification pom openfeign 0、业务代码 topic 字符串硬编码 0（全经 `Topics` 常量）、jmx `ResponseAssertion` 0 个（XML 解析通过）。
- 契约方向：6 个 Feign 契约（AuthApi/PatientApi/NoduleApi/FollowupApi/NotifyApi/AiApi/FileApi）全部绝对路径、实现类无类级 `@RequestMapping`。
- 版本无漂移：P5 子代理依赖树核对 sentinel-core 1.8.8、minio 8.5.7、spring-cloud-context 4.1.4 与 BOM 声明一致。
- 安全不变量抽查：抽取 0→1 仅回填、confirm 1→2 才建结节（无自动确认路径）；confirm 排期失败不回滚仅记 planError；幂等三保险（SETNX+唯一键+release 后 rethrow）单测证据在位。

## 3. 初步差异观察（供五维度评审）

### 3.1 逐字命中（零差异或仅注释差异）

- 6 份 DDL 中 3 份仅头部注释差异：yiliao_ai.sql（1 行）、yiliao_stats.sql（3 行）、yiliao_notify.sql（2 行：头部注释 + `MDADI/MDASI`，执行书写 MDADI、主仓 MDASI——执行书编写时未核到该字）。
- 全部 yml 全文规格项、BOM/版本属性、错误码（10001-10005/24001/24002/26001-26003/11001）、消息模板、Prompt、正则、RRF_K=60/SCALE=4 等均命中。

### 3.2 差异清单（含归因）

| # | 位置 | 重放值 | 主仓值 | 归因 |
|---|---|---|---|---|
| D1 | common-mq `Topics` 常量值 | 小写 `yiliao_remind_due` 等 | 大写 `YILIAO_REMIND_DUE` 等（常量名同为短名） | P0 骨架取值 vs 主仓；P3 子代理仅改名不改值（合理） |
| D2 | `TaskDatesPlannerTest` | START=2025-07-10、末点 2030-01-10 | START=2026-01-10、末点 2030-07-10 | 执行书明示"固定 start 如 2025-07-10"，**合法 brief 级差异** |
| D3 | 测试总数 | 115 | 143（差 28） | 主仓 P1b/P3b/P4b/P5b 增量（PatientDataGuard 7、PatientRoleGuard 5、gateway +5、common-core 7、common-web 4）；README 已声明 b 增量不回移，**已知差异非缺陷** |
| D4 | P0 测试 | 0 | 11（PageQuery 4+Result 3+GlobalExceptionHandler 4） | **执行书缺陷**：P0 测试清单未含 common 层测试 |
| D5 | compatibility-verifier | 未配置（服务无法启动，构建/测试不受影响） | 各服务 yml `spring.cloud.compatibility-verifier.enabled: false` | **执行书缺陷**：Boot 3.4.5 vs SC 2023.0.3 release-train 校验器 P1 起即在 classpath，P5 首次真启动才暴露；主线 P4/P5 集成时已在主仓修，未写入执行书 |
| D6 | file 错误转换 | `SYSTEM_BUSY(500).withMsg(...)` | 执行书引用 `SYSTEM_ERROR` | 重放 P0 骨架枚举名与主仓不一致，子代理按最小合理实现选 SYSTEM_BUSY，文案逐字保留 |
| D7 | docker-compose | sentinel-dashboard 展开为完整注释块 | 单行占位注释 | P5 子代理越出执行书 §1 清单的自报改动（使演示步骤可执行） |
| D8 | **P2 三份 DDL（patient/nodule/followup）** | **与主仓字节级相同（cmp=0）** | — | **纯度异常**：收尾代理自述 DDL 来源存疑（疑似参照主仓），该三项"逐字比对"维度对 P2 **无效评分** |
| D9 | auth yml `exclude: RedissonAutoConfiguration` | 保留（无效引用，不阻断启动） | 同左（主仓同样遗留） | P1 遗留，两侧一致，非重放引入 |

### 3.3 外部模型踩坑记录（论文素材）

- P2（kimi）：命中执行书已预警的两个陷阱——MyBatis-Plus `insert` 重载 mock 歧义、Redisson `tryLock` 重载 mock；且 TaskDatesPlannerTest 期望值算错（2028-01-10，应为 2030-01-10，间隔 6 首查 + 12 重复 ×5 点），均由收尾代理修正、主线独立验算确认。
- P3（通用子代理）：首版幂等测试断言计数写错，自修正后通过（子代理自报）。
- P4/P5（通用子代理）：各 5~6 处"执行书未明写处的最小合理实现"，均已按执行书要求列入其报告"未决问题"（如 ChatService `onErrorResume`、KnowledgeBaseService 第 4 构造参、ConfirmService 对 `readValue("null")` 补判）。

## 4. 偏差披露（必读，影响实验有效性判定）

1. **执行模型构成**：纯 kimi = P0、P1；kimi+通用子代理混合 = P2；纯通用子代理 = P3、P4、P5。kimi 供应商持续故障（7 次瞬时鉴权失败），按预先约定（P2 时确立、用户未反对）降级切换。若论文实验设定为"某指定外部模型重放"，则当前数据实际是**两个执行体的混合样本**——建议表述为"kimi（P0~P1）/ 通用代理（P3~P5）双样本"，或在 kimi 恢复后从 `0aba0c9` 重放 P3~P5 补齐同模型样本。
2. **P2 DDL 来源**：3 份 DDL 与主仓字节级一致，收尾代理自述无法排除参照主仓。P2 的逐字比对维度中 DDL 项应记"无效/存疑"而非满分。
3. **子代理自修正**：P3/P4/P5 子代理存在少量测试断言自修正（自报），主线验证的是修正后终态；"一次通过率"指标无法精确统计，论文引用时注意口径。
4. **纯度约束**：P3 起对子代理施加了"禁读主仓"硬约束；P2 收尾代理**未**施加该约束（时序原因），这是 D8 的成因，也是 P3~P5 与 P2 的可比性差异。

## 5. 待主线决策

1. **五维度评审**可开始；D8 三项（P2 DDL）建议标注"无效评分"。
2. 是否补 `compatibility-verifier` 项进 P0/P1 执行书（README 维护规则为"阶段首版基线不回移"，但此项属 P1 底座缺失而非 b 增量；补则需重放，不补则在论文中记"执行书已知缺陷 1 处"）。
3. 是否补 P0 执行书测试清单（common 11 测试，D4）。
4. kimi 恢复后是否重放 P3~P5（目录 `git reset --hard 0aba0c9` 后从 P3 执行书重跑；P3~P5 执行书未改动，可直接复用）。
5. auth yml 无效 Redisson exclude（D9，主仓同样存在）是否顺手修。
