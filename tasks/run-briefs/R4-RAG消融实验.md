# R4：RAG 消融实验（纯向量 vs 混合检索 vs 混合+Rerank）

> 类型：评测实验（可新建测试与评测文件，不改业务代码）　前置：无中间件要求（评测在单测级别运行）；如已配真 LLM Key 可顺带跑真实版。

## 目标

产出论文级消融实验表：同一评测集（40 题肺结节随访知识问答）上对比三种检索配置的命中率（答案所需章节是否进入 Top3）。表驱动证据素材（specs/modules/ai.md §4.4）。

## 实现位置（白名单）

- 新建：`yiliao-ai/src/test/java/com/yiliao/ai/rag/RagAblationEval.java`（JUnit 测试，断言改为输出报告）
- 新建：`tasks/eval/rag-eval-set.jsonl`（评测集，40 题）与 `tasks/evidence/R4-ablation-table.md`

## 步骤

### S1 评测集

按指南种子内容构造 40 题 JSONL（每行 `{"q":"...","expectSection":"随访","expectKeyword":"6个月"}`）：
- 实性结节 ≤4/4-6/6-8mm 有无危险因素各 3~4 题（≥12 题）
- pGGN ≤5/>5、mGGN ≤8/>8 各 4 题（≥16 题）
- 术后随访（原位癌/I 期/II-III 期/EGFR 辅助/IV 期/小细胞局限期）≥12 题
问题用口语化问法（"我这种结节多久复查一次"）与术语问法（"6-8mm 实性结节伴吸烟史随访间隔"）各半。

### S2 知识库

导入内容与 R1 T3 同源的扩展版指南文本（覆盖种子规则全部分支的指南段落汇总，含章节标题）。

### S3 实验

RagAblationEval 中按三组配置跑：
- **A 纯向量**：只用 DeterministicEmbedding 余弦（调用 HybridRetriever.retrieve 时把 keyword 通道权重置 0 的方式：对候选直接取向量分排序——评测代码内自行构造两个通道分离的检索器实例，不改主代码）
- **B 混合检索（RRF）**：现有 retrieve 默认行为
- **C 混合+Rerank**：retrieve + reRank（当前 reRank 为透传钩子——C 组用"章节标题包含题干关键词加权重排"的简易精排实现于评测类内，标注为演示版）

指标：命中率 = 期望章节进入 Top3 的题目占比；关键词命中率 = 首条结果文本含 expectKeyword 的占比。输出 Markdown 表格到 tasks/evidence/R4-ablation-table.md：

| 配置 | Top3 章节命中率 | 首条关键词命中率 |
|---|---|---|
| A 纯向量 | … | … |
| B 混合 RRF | … | … |
| C 混合+Rerank | … | … |

### S4 结论段

表格下附 3~5 行结论：精确术语类问题（"4-6mm"）上纯向量弱于混合（bigram 关键词通道救回）；Rerank 提升幅度；伪向量局限说明（P4b 换 BGE 后重跑，数字预计整体上升但相对趋势不变）。

## 验证表

| 检查 | 命令 | 通过标准 |
|---|---|---|
| 评测可复现 | `mvn test -pl yiliao-ai -Dtest=RagAblationEval -f F:\bc\yiliao\pom.xml` | 通过且输出表 |
| 实验表 | 查看 tasks/evidence/R4-ablation-table.md | 三组数字 + 结论段 |

## 禁止

不改 HybridRetriever/KnowledgeBaseService 主代码（评测所需变体在测试类内实现并注明）；不虚构数字——评测集必须先入库后跑分。
