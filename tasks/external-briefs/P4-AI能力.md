# P4 AI 能力 · 外部模型执行书（自包含）

> 用途：交给另一个模型执行 P4（yiliao-ai：LLM 抽取/报告解读/RAG/工具调用 + nodule 确认排期链路）。
> ⚠️ **前置**：必须在已完成 P0 + P1 + P2 + P3 执行书的**同一目录**内继续（15 模块已构建通过）。
> ⚠️ 不要在现有 `F:\bc\yiliao` 仓库内操作（P0~P5 已完成，会冲突）。
> 你是专注执行的子代理：只做本文指派的事；有歧义先列出理解请求确认；不新增未授权依赖；完成后按"输出要求"报告。

---

## 1. 目标与边界

**目标**：AI 能力首版 + 医生确认排期主线（"AI 只起草、人确认"不变量的落地），满足验收：
- `mvn install` 全绿（16 个模块：P3 的 15 个 + yiliao-ai）
- 脱敏单测：手机号/身份证/邮箱/住院号被替换，正常文本不误改
- 抽取单测：fake LLM 返回 JSON→DTO 映射；坏 JSON 重试 1 次后 24002；Prompt 含脱敏后文本
- RAG 单测：混合检索把相关章节排首位、RRF 双通道命中优先、切分正确
- 工具安全单测：工具内部用当前登录用户 id（LLM 无法越权）
- nodule 确认单测：状态流转 0→1→2、建结节+快照、自动排期被调用

**不在范围（P4b）**：Seata @GlobalTransactional（当前同步 Feign 容错模式继续）、PgVector 实装（内存库 + 确定性伪向量）、Redis 会话记忆、Rerank 服务、file-service（P5）。

**本阶段新增/修改**：
- 新增：`yiliao-ai/**`、`docker/init-sql/yiliao_ai.sql`、yiliao-api ai 包、nodule 的 extract/confirm 端点与 ConfirmService
- 修改：根 pom（modules +1）、gateway 路由（+ai）

## 2. 硬约束（违反即返工）

1. P0~P3 全部硬约束继续有效。
2. **AI 只起草、人确认**（医疗安全底线）：AI 抽取结果只是草稿（`extract_status` 0→1），必须经医生调 confirm 接口人工确认后（1→2）才写入 nodule/nodule_snapshot；禁止任何"自动确认"路径。
3. **AI 输出必须标注"辅助参考"**：报告解读与 RAG 问答的 SSE 流末尾，服务端强制追加免责语（不依赖 LLM 自觉）；RAG 回答必须附指南出处（citation 事件），知识库不可用时明确告知而非自由发挥。
4. **PII 出网过滤**：发给 LLM 前的文本必须过脱敏过滤器（手机号/身份证/邮箱/住院号）；脱敏后仍检出敏感残留 → 终止出网抛错。
5. **工具越权防护**：4 个 @Tool 方法**不接受也不信任** LLM 提供的 patientId——一律取网关透传的 `X-User-Id` 头（当前登录患者），越权在 Java 层拦截，不靠 Prompt。
6. **抽取缺失禁臆造**：LLM Prompt 明确"原文没有的字段返回 null，禁止臆造或推算"；解析失败重试 1 次（附纠错提示），再失败抛 24002 不阻塞人工录入。
7. LLM 可插拔：OpenAI 兼容协议，模型只改配置（base-url/model/api-key 环境变量）不改代码。
8. LLM 调用抽象走 `LlmClient` 接口（chat/streamChat/chatForJson），业务不直接依赖 Spring AI 类——离线测试用内置 Fake 实现。

## 3. 已知陷阱（先记再写）

1. P0~P3 全部陷阱继续有效。
2. **Spring AI 1.0.0 API**：`ChatClient.builder(chatModel).build()`；`.prompt().system(...).user(...).call().content()`；流式 `.stream().content()` 返回 `Flux<String>`（doOnNext 逐块）。依赖 artifact 是 `org.springframework.ai:spring-ai-starter-model-openai`（由根 POM 的 spring-ai-bom 管版本）。
3. **SSE**：`SseEmitter` + `emitter.send(SseEmitter.event().name("delta").data(...))`；端点 `produces = MediaType.TEXT_EVENT_STREAM_VALUE`；超时后 complete；异常分支先补发降级文案再 done/complete。
4. **Jackson 解析 record**：NoduleExtract 是 record，缺失 JSON 字段 → 组件 null（正好满足"缺失=null"）；额外字段需 `FAIL_ON_UNKNOWN_PROPERTIES=false` 或严格匹配。
5. **确定性伪向量**：Embedding 默认实现 = bigram 词频 → 128 维 → L2 归一化（离线可演示、语义退化为词频相似）；接口保留，P4b 换真模型只改 Bean 不改业务。
6. **Feign 契约铁律**继续有效：AiApi 方法绝对路径，AiApiController 无类级 @RequestMapping。

## 4. 执行步骤（按序）

### 步骤 1：根 pom.xml

`<modules>` 在 `yiliao-notification` 之后追加：`<module>yiliao-ai</module>`

### 步骤 2：yiliao-ai（8086）

**2.1 pom.xml**（artifactId=yiliao-ai）：
```xml
com.yiliao:yiliao-common-core
com.yiliao:yiliao-common-web
com.yiliao:yiliao-common-data
com.yiliao:yiliao-api
org.springframework.cloud:spring-cloud-starter-openfeign
<!-- Spring AI：OpenAI 兼容协议，模型可插拔（specs/modules/ai.md §4.7） -->
org.springframework.ai:spring-ai-starter-model-openai
com.mysql:mysql-connector-j (runtime)
com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery
org.springframework.boot:spring-boot-starter-test (test)
```

**2.2 application.yml**（全文即规格）：
```yaml
server:
  port: 8086

spring:
  application:
    name: yiliao-ai
  datasource:
    url: jdbc:mysql://localhost:3306/yiliao_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: yiliao123
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
  ai:
    openai:
      # OpenAI 兼容协议：DeepSeek/通义/豆包换 base-url + model 即可（specs/modules/ai.md §4.7）
      # api-key 生产走 Nacos/环境变量，仓库占位
      api-key: ${YILIAO_LLM_API_KEY:sk-dev-placeholder}
      base-url: ${YILIAO_LLM_BASE_URL:https://api.deepseek.com}
      chat:
        options:
          model: ${YILIAO_LLM_MODEL:deepseek-chat}
          temperature: 0.2
  cloud:
    nacos:
      discovery:
        enabled: false
        server-addr: localhost:8848
  autoconfigure:
    exclude:
      - org.redisson.spring.starter.RedissonAutoConfiguration

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    banner: false
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

yiliao:
  ai:
    rag:
      top-k: 3
      chunk-size: 600
      chunk-overlap: 100

knife4j:
  enable: true

logging:
  level:
    com.yiliao: INFO
```

**2.3 包结构与类规格**（`com.yiliao.ai`）：
- `YiliaoAiApplication`：`@SpringBootApplication @EnableFeignClients(basePackages = "com.yiliao.api")`
- `error.AiErrorCode`：
  - `LLM_DEGRADED(24001, "AI 服务暂时不可用，请稍后再试或联系您的医生")`
  - `EXTRACT_FAILED(24002, "报告抽取失败，请手动录入")`
- `llm/SensitiveDataFilter`（`public final class` 私有构造，纯静态；javadoc：姓名无法可靠识别，靠 Prompt 约束补位）：
  - 规则清单（**顺序即优先级：长模式必须先于短模式**，逐字）：
```java
private static final List<Rule> RULES = List.of(
        new Rule(Pattern.compile("\\d{17}[0-9Xx]"), "[证件号]"),
        new Rule(Pattern.compile("\\d{15}"), "[证件号]"),
        new Rule(Pattern.compile("1[3-9]\\d{9}"), "[电话]"),
        new Rule(Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+"), "[邮箱]"),
        new Rule(Pattern.compile("(住院号|门诊号|病历号)[:：\\s]*\\d+"), "$1[已隐藏]")
);
```
  - `public static String sanitize(String text)`：null/空直接返回；按序 replaceAll
  - `public static boolean containsSensitive(String text)`：仅自检前 3 条（证件号×2 + 手机号）anyMatch find
- `llm/LlmClient`（接口 + 内置测试实现）：
```java
public interface LlmClient {
    String chat(String systemPrompt, String userPrompt);
    void streamChat(String systemPrompt, String userPrompt, Consumer<String> onDelta);
    String chatForJson(String systemPrompt, String userPrompt);

    /** 测试用 fake：chatForJson 返回固定 json，streamChat 逐块回调固定文本。 */
    record Fake(String jsonReply, String textReply) implements LlmClient { ... }
}
```
- `llm/SpringAiLlmClient`（`@Component implements LlmClient`）：构造注入 Spring AI 自动装配的 `ChatModel`，`this.chatClient = ChatClient.builder(chatModel).build()`；chat = `.prompt().system(...).user(...).call().content()`；streamChat = `.stream().content().doOnNext(onDelta).blockLast()`；chatForJson = chat + 追加 `"\n\n只输出 JSON，不要输出任何其他文字或代码块标记。"`
- `extract/ReportExtractService`（`@Service`，注入 LlmClient）：
  - Prompt（**逐字**）：
```java
static final String SYSTEM_PROMPT = """
        你是放射科结构化助手。从 CT 报告原文抽取肺结节信息，输出严格 JSON：
        {"location":"位置","noduleType":"SOLID|PART_SOLID|PURE_GG","maxDiameterMm":数值,
         "solidDiameterMm":数值或null,"count":数值,"signs":["恶性征象"],"impression":"影像结论"}
        规则：只抽取原文明确存在的信息；原文没有的字段返回 null，禁止臆造或推算；数值不带单位。
        """;
static final String RETRY_HINT = "\n上次输出不是合法 JSON，请重新只输出符合 schema 的 JSON。";
```
  - `public NoduleExtract extract(String rawText)`：
    1. `String sanitized = SensitiveDataFilter.sanitize(rawText);`
    2. `containsSensitive(sanitized)` 仍 true → `log.error("脱敏后仍检出敏感残留，终止出网（不应发生）")` + 抛 `BizException(EXTRACT_FAILED)`
    3. `parse(llmClient.chatForJson(SYSTEM_PROMPT, sanitized))`
    4. 首次解析异常（非 BizException）→ `log.warn("抽取首次解析失败，重试一次")`，用 `sanitized + RETRY_HINT` 重试一次
    5. 再失败 → 抛 `BizException(EXTRACT_FAILED)`（24002，不阻塞人工录入）
  - `static NoduleExtract parse(String json)`（容错剥代码块，逐字）：
```java
String cleaned = json == null ? "" : json.trim()
        .replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
return MAPPER.readValue(cleaned, NoduleExtract.class);
```
- `interpret/ReportInterpretService`（`@Service`，注入 LlmClient）：
  - Prompt（逐字）：
```java
static final String SYSTEM_PROMPT = """
        你是面向患者的胸外科随访助手，用通俗语言解读 CT 报告。固定输出四段：
        【这是什么】【风险怎么看】【下一步建议】【注意事项】。
        要求：只基于报告内容解释，不臆造；语气平和避免焦虑；不给出确定性诊断结论。
        """;
public static final String DISCLAIMER = "\n\n—— 以上内容为 AI 辅助参考，不构成诊断意见，请以主治医生意见为准。";
```
  - `public SseEmitter interpret(String rawText)`：`new SseEmitter(60_000L)`；先 sanitize + 敏感残留检查（同抽取）；`llmClient.streamChat(SYSTEM_PROMPT, sanitized, delta -> emitter.send(event().name("delta").data(delta)))`；流完**服务端强制**追加 `delta`(DISCLAIMER) → `done`(data=`"[DONE]"`) → complete；catch Exception 优雅降级：发 `delta`(LLM_DEGRADED.getMsg()) 再 done/complete（降级路径内 IOException 才 completeWithError）
- `rag/TextChunker`（构造 `TextChunker(int chunkSize, int overlap)`，`chunkSize <= overlap` 抛 `IllegalArgumentException("chunkSize 必须大于 overlap")`）：
  - `record Chunk(String docName, String section, String text) {}`
  - `public List<Chunk> chunk(String docName, String content)`：按行扫，行 trim 后 `startsWith("#")` 视为新章节（flush 后 `section = trimmed.replaceAll("^#+\\s*", "")`）；否则追加进 buffer；`buffer.length() >= chunkSize` 即 flush；无标题 section 初值 `"正文"`
  - flush：`text.length() <= chunkSize` 直接成块；超长滑窗：`step = chunkSize - overlap`，`for (start=0; start<len; start+=step) { end = min(len, start+chunkSize); add(substring(start, end)); if (end == len) break; }`
- `rag/HybridRetriever`（`@Component`；javadoc：混合检索 = 关键词 bigram + 向量余弦，RRF 融合）：
  - `public static final int RRF_K = 60;`
  - `public record DocChunk(String docName, String section, String text, double[] embedding) {}`
  - `void add(DocChunk chunk)`；`int size()`
  - `public List<DocChunk> retrieve(String query, double[] queryEmbedding, int topK)`：
    - 关键词通道：`keywordScore(query, text)` 降序
    - 向量通道：queryEmbedding != null 时 `cosine(queryEmbedding, embedding)` 降序
    - **RRF 融合（逐字）**：
```java
Map<String, Double> fused = new HashMap<>();
for (int rank = 0; rank < keyword.size(); rank++) {
    fused.merge(keyword.get(rank).getKey(), 1.0 / (RRF_K + rank + 1), Double::sum);
}
for (int rank = 0; rank < vector.size(); rank++) {
    fused.merge(vector.get(rank).getKey(), 1.0 / (RRF_K + rank + 1), Double::sum);
}
return fused.entrySet().stream()
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
        .limit(topK)
        .map(e -> store.get(e.getKey()))
        .toList();
```
  - bigram 与关键词分（逐字）：
```java
static Map<String, Integer> bigrams(String text) {
    Map<String, Integer> counts = new HashMap<>();
    String normalized = text.replaceAll("\\s+", "");
    for (int i = 0; i + 2 <= normalized.length(); i++) {
        counts.merge(normalized.substring(i, i + 2), 1, Integer::sum);
    }
    return counts;
}

static double keywordScore(String query, String text) {
    if (query == null || query.isBlank()) {
        return 0;
    }
    Map<String, Integer> queryGrams = bigrams(query);
    Map<String, Integer> textGrams = bigrams(text);
    double overlap = 0;
    for (Map.Entry<String, Integer> entry : queryGrams.entrySet()) {
        overlap += Math.min(entry.getValue(), textGrams.getOrDefault(entry.getKey(), 0));
    }
    return overlap / queryGrams.size();
}
```
  - 余弦（逐字）：
```java
static double cosine(double[] a, double[] b) {
    if (a == null || b == null || a.length != b.length) {
        return 0;
    }
    double dot = 0;
    double normA = 0;
    double normB = 0;
    for (int i = 0; i < a.length; i++) {
        dot += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    if (normA == 0 || normB == 0) {
        return 0;
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}
```
  - Rerank 钩子：`public List<DocChunk> reRank(String query, List<DocChunk> candidates) { return candidates; }`（默认透传，P4b 接 bge-reranker）
- `rag/KnowledgeBaseService`（`@Service`，构造 `@Value("${yiliao.ai.rag.top-k:3}") int topK, @Value("${yiliao.ai.rag.chunk-size:600}") int chunkSize, @Value("${yiliao.ai.rag.chunk-overlap:100}") int overlap`）：
  - `public int importDoc(String docName, String content)`：切分 → 每 chunk `retriever.add(new DocChunk(docName, section, text, DeterministicEmbedding.embed(text)))`，返回 chunks.size()
  - `public List<DocChunk> retrieve(String query)`：`retriever.reRank(query, retriever.retrieve(query, DeterministicEmbedding.embed(query), topK))`
  - `public boolean isEmpty()`
  - 内部 `static final class DeterministicEmbedding`：`DIM = 128`；`embed(String)` = bigram 词频 `vector[Math.floorMod(gram.hashCode(), DIM)] += count` 后 L2 归一化（javadoc：离线可演示、语义退化为词频相似、P4b 换 BGE Embedding 保留同接口）
- `chat/ChatService`（`@Service`，注入 `ChatClient.Builder`、KnowledgeBaseService、PatientTools）：
  - `public static final String DISCLAIMER = "\n\n—— 以上为 AI 辅助参考，不替代诊断；具体请遵主治医生意见。";`
  - `public SseEmitter chat(String message)`：`new SseEmitter(120_000L)`；知识库空 → contexts=List.of()，否则 `knowledgeBase.retrieve(message)`；`chatClientBuilder.build().prompt().system(buildSystemPrompt(contexts)).user(message).tools(patientTools).stream().content()` 流式：doOnNext 发 `delta`；doOnComplete 发 `citation`（buildCitation）→ `delta`(DISCLAIMER) → `done`(`[DONE]`)；doOnError 发 `delta`("AI 服务暂时不可用，请稍后再试或联系您的医生。") + `done`
  - 引用组装（逐字）：
```java
static String buildCitation(List<HybridRetriever.DocChunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
        return "";
    }
    StringBuilder sb = new StringBuilder("\n\n参考指南：");
    for (HybridRetriever.DocChunk chunk : chunks) {
        sb.append("\n· ").append(chunk.docName()).append(" · ").append(chunk.section());
    }
    return sb.toString();
}
```
  - buildSystemPrompt 固定段（核心逐字）：
```
你是肺结节/肺癌随访助手，服务对象是患者与医生。规则：
1. 涉及诊疗建议时，只依据提供的指南片段作答并注明出处；片段中没有的就说"指南片段未覆盖"，不得编造。
2. 涉及患者个人数据（随访计划/结节趋势/报告结论/症状上报）时，调用提供的工具查询真实数据后再回答。
3. 不给确定性诊断结论；语气平和，避免引起焦虑。
```
    知识库为空追加：`"\n当前知识库不可用：如被问及指南知识，明确告知"知识库暂不可用，建议咨询医生"。\n"`；否则追加「以下是检索到的指南片段：」+ 每片段 `【docName·section】text\n\n`
  - `tools/PatientTools`（`@Component`，注入 FollowupApi/NoduleApi；类注释：安全底线——patientId 一律取 X-User-Id，工具方法**不接受也不信任** LLM 提供的任何 patientId）：
```java
@Tool(description = "查询当前患者本人的下次随访计划（日期与项目）")
public NextFollowupDTO queryNextFollowup() {
    Long patientId = currentPatientId();
    return followupApi.nextFollowup(patientId).data();
}

@Tool(description = "查询当前患者本人的结节变化趋势（历次复查快照）")
public List<SnapshotDTO> queryNoduleTrend() {
    Long patientId = currentPatientId();
    return noduleApi.getTrend(patientId, null).data();
}

@Tool(description = "上报症状。symptom：症状名；severity：严重程度 1-5")
public String reportSymptom(@ToolParam(description = "症状，如 咳嗽/疼痛/乏力") String symptom,
                            @ToolParam(description = "严重程度 1-5 的整数") int severity) {
    Long patientId = currentPatientId();
    if (severity < 1 || severity > 5) {
        return "severity 必须在 1-5 之间";
    }
    Long id = followupApi.reportSymptom(patientId,
            new FollowupApi.SymptomRequest(symptom, severity, "AI 助手上报", 2)).data();
    return severity >= 3
            ? "症状已上报（编号 " + id + "），严重程度较高，已通知您的医生关注。"
            : "症状已上报（编号 " + id + "）。";
}

@Tool(description = "查询当前患者本人最近一次检查报告的结论摘要")
public String queryReportSummary() {
    Long patientId = currentPatientId();
    String summary = noduleApi.reportSummary(patientId).data();
    return summary == null ? "暂无检查报告记录" : summary;
}

/** 当前登录患者 id：只认网关透传头，LLM 无法注入。 */
static Long currentPatientId() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
        String userId = attributes.getRequest().getHeader(YiliaoConstants.HEADER_USER_ID);
        if (userId != null && !userId.isBlank()) {
            return Long.valueOf(userId);
        }
    }
    throw new BizException(CommonErrorCode.UNAUTHORIZED);
}
```
- `entity.KnowledgeDoc`：`@TableName("knowledge_doc")` extends BaseEntity；字段 `docName, version, chunkCount, Integer status（默认 1）`
- `mapper.KnowledgeDocMapper`：`@Mapper extends BaseMapper<KnowledgeDoc>`
- `controller/AiController`（`@RestController @RequestMapping("/api/ai")`）：
  - `POST /api/ai/report/extract`，`record ExtractRequest(@NotBlank String rawText)` → `Result<NoduleExtract>`
  - `POST /api/ai/report/interpret`（`produces = TEXT_EVENT_STREAM_VALUE`），`ExtractRequest` → `SseEmitter`
  - `POST /api/ai/chat`（`produces = TEXT_EVENT_STREAM_VALUE`），`record ChatRequest(@NotBlank String message)` → `SseEmitter`
  - `GET /api/ai/knowledge/status` → `Result<Integer>`（空 0 / 非空 1）
- `controller/KnowledgeController`（`@RestController @RequestMapping("/api/ai/knowledge")`）：
  - `POST /api/ai/knowledge/import`，`record ImportRequest(@NotBlank String docName, String version, @NotBlank String content)` → `Result<Integer>`（返回切片数）：`knowledgeBase.importDoc` → 组 KnowledgeDoc(status=1, chunkCount) → insert
- `feign/AiApiController`：`@RestController implements AiApi`（无类级 @RequestMapping）：`extractReport` 转调 `extractService.extract(request.rawText())`
- **测试（4 个测试类）**：
  - `llm/SensitiveDataFilterTest`（纯 JUnit），5 用例：
    - `phoneMasked` — 13812345678 → `[电话]` 且 containsSensitive=false
    - `idCardMasked` — 18 位证件号 → `[证件号]`
    - `emailMasked` — doctor@example.com 被替换
    - `medicalRecordNumberMasked` — 「住院号：20260822001」号码部分 → `[已隐藏]`
    - `normalTextUntouched` — 正常医学描述（含 5mm）不被误改
  - `extract/ReportExtractServiceTest`（用 LlmClient.Fake），5 用例：
    - `parsesStructuredResult` — Fake 返 GOOD_JSON：location/noduleType/maxDiameterMm/signs 正确且 solidDiameterMm=null（缺失禁臆造）
    - `promptIsSanitizedBeforeSend` — 捕获 chatForJson 的 userPrompt：含 `[电话]` 且不含原始手机号
    - `badJsonRetriesOnceThenFails` — Fake 恒返坏 JSON：抛 24002 且 chatForJson 恰被调 2 次
    - `recoversOnSecondAttempt` — 第一次坏、第二次 GOOD_JSON → 成功
    - `jsonCodeFenceStripped` — parse 能剥 ```json 代码块
  - `rag/RagPipelineTest`（纯 JUnit 手工构造知识库，文档含 `# 实性结节`/`# 磨玻璃结节` 两章节），6 用例：
    - `hybridRetrievalRanksRelevantSectionFirst` — 查「磨玻璃结节5mm多久复查一次」Top1 = 磨玻璃章节
    - `unrelatedQueryReturnsSomethingButKnowledgeBaseEmptyGuardExists` — 初始 isEmpty=true，导入后检索返回非空
    - `chunkerSplitsBySectionHeaders` — `#` 标题切成 2 块，section 名正确
    - `chunkerSlidingWindowForLongText` — TextChunker(50,10) 对 200 字长文切多块且每块 ≤50 字
    - `chunkerRejectsBadConfig` — new TextChunker(100,100) 抛 IllegalArgumentException
    - `rrfFusionPrefersDualChannelHits` — 双通道命中文档 RRF 分最高排第一
  - `tools/PatientToolsTest`（`@ExtendWith(MockitoExtension.class)`，MockHttpServletRequest 加头 `X-User-Id: 1001`），4 用例：
    - `queryNextFollowupUsesCurrentUserNotLlmSupplied` — 工具签名不含 patientId，Feign 收到 1001
    - `reportSymptomValidatesSeverityRange` — severity=9 → "severity 必须在 1-5 之间"
    - `reportSymptomHighSeverityNotifiesDoctor` — severity=3 → 文案含「已通知」且 Feign 被调
    - `noUserContextRejected` — 无请求上下文抛 BizException（UNAUTHORIZED）

### 步骤 3：yiliao-api ai 契约

- `api/ai/AiApi.java`（全文即规格）：
```java
@FeignClient(name = "yiliao-ai", contextId = "aiApi")
public interface AiApi {

    @PostMapping("/api/ai/internal/report/extract")
    Result<NoduleExtract> extractReport(@RequestBody ExtractRequest request);

    record ExtractRequest(@NotBlank String rawText) {
    }
}
```
- `api/ai/package-info.java`：一句"ai 服务对外契约包：AiApi（extractReport）——P4 实现；NoduleExtract DTO 属 P0 骨架"注释
- 说明：`NoduleExtract` DTO 已存在于 P0 骨架（`api/ai/dto/NoduleExtract.java`），本阶段**不重复创建**。

### 步骤 4：nodule 确认排期链路（F1 主线）

nodule 服务新增 2 个端点 + ConfirmService（P2 已建 exam_report 表与报告录入端点）：

**4.1 `controller.NoduleController` 追加**（现有类上）：
```java
@Operation(summary = "触发 AI 抽取（草稿回填 structured_json，状态 0→1，不入库）")
@PostMapping("/exam/reports/{id}/extract")
public Result<NoduleExtract> extract(@PathVariable Long id) {
    ExamReport report = reportMapper.selectById(id);
    if (report == null) {
        return Result.fail(NoduleErrorCode.REPORT_NOT_FOUND);
    }
    NoduleExtract extract = aiApi
            .extractReport(new AiApi.ExtractRequest(report.getRawText())).data();
    report.setStructuredJson(toJson(extract));
    report.setExtractStatus(1);
    reportMapper.updateById(report);
    return Result.ok(extract);
}

@Operation(summary = "医生确认 AI 抽取结果入库（建结节+快照+自动排期）")
@PostMapping("/exam/reports/{id}/confirm")
public Result<ConfirmService.ConfirmResult> confirm(
        @PathVariable Long id,
        @RequestParam(required = false, defaultValue = "false") boolean newNodule,
        @RequestParam(required = false) Integer isNewFlag) {
    return Result.ok(confirmService.confirm(id, newNodule, isNewFlag));
}
```

**4.2 `confirm/ConfirmService`**（`@Service`，注入 nodule/snapshot/report/patient 相关 Mapper + PatientApi/FollowupApi）：
- `TYPE_CODE = Map.of("SOLID", 1, "PART_SOLID", 2, "PURE_GG", 3);`
- `@Transactional public ConfirmResult confirm(Long reportId, boolean isNewNodule, Integer isNewFlag)`：
  1. report null → `REPORT_NOT_FOUND`；`extractStatus == 2` → `ALREADY_CONFIRMED`
  2. `MAPPER.readValue(structuredJson, NoduleExtract.class)` 失败 → `STRUCTURED_JSON_INVALID.withMsg("抽取结果缺失或不合法，请先执行抽取")`
  3. 建结节：同患者结节 count → `noduleNo = "N" + (count + 1)`；location=extract.location；`noduleType = TYPE_CODE.getOrDefault(extract.noduleType(), 1)`（未知字符串兜底 1）；status=1；firstFoundDate=report.examDate；insert
  4. 首条基线快照：noduleId/reportId/examDate/maxDiameterMm/solidDiameterMm；`signs == null ? null : String.join("、", signs)`；`isNew = isNewFlag == null ? 0 : isNewFlag`；insert
  5. `report.setExtractStatus(2)` + updateById（1→2）
  6. Feign 自动排期（**失败不回滚**，javadoc：失败不阻断确认，医生可手动触发 plans/generate；P4b Seata 强化）：
```java
Long planId = null;
String planError = null;
try {
    Boolean risk = patientApi.hasRiskFactor(report.getPatientId()).data();
    RuleInputDTO input = new RuleInputDTO(nodule.getNoduleType(),
            orDefault(extract.maxDiameterMm()), Boolean.TRUE.equals(risk), null, null, null);
    planId = followupApi.generatePlan(new PlanGenerateRequest(
            report.getPatientId(), 1, input, LocalDate.now().toString())).data();
} catch (Exception e) {
    planError = e.getMessage();
    log.warn("确认后排期失败 reportId={} msg={}", reportId, e.getMessage());
}
return new ConfirmResult(nodule.getId(), snapshot.getId(), planId, planError);
```
  - `orDefault`：null → `BigDecimal.ZERO`
  - `public record ConfirmResult(Long noduleId, Long snapshotId, Long planId, String planError) {}`
- 测试 `ConfirmServiceTest`（`@ExtendWith(MockitoExtension.class)`，mock 三 mapper + PatientApi/FollowupApi），4 用例：
  - `confirmCreatesNoduleSnapshotAndPlan` — 建结节 N1、PURE_GG→type 3、snapshotId/planId 回填、extractStatus 1→2、generatePlan 入参 riskFactor=false（PatientApi 真实值）与 maxDiaMm
  - `planFailureDoesNotRollbackConfirm` — followup 抛异常：extractStatus 仍=2、planId=null、planError 非空
  - `doubleConfirmRejected` — extractStatus=2 再确认抛 21004
  - `missingStructuredJsonRejected` — structured_json=null 抛 21003

### 步骤 5：docker/init-sql/yiliao_ai.sql（全文即规格）

```sql
-- yiliao_ai 库：会话/消息表 P4b 落库（本期表结构就绪，代码不读写会话）
-- 向量切片 P4b 迁 PgVector，本期 chunk 在服务内存库（重启重导入，演示足够）

USE yiliao_ai;

CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT,
  doctor_id BIGINT,
  title VARCHAR(128),
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 会话（P4b 落库）';

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(16) COMMENT 'user/assistant/tool',
  content TEXT,
  tool_calls_json JSON COMMENT '工具调用审计',
  citations_json JSON COMMENT 'RAG引用出处',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_session(session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 消息（P4b 落库）';

CREATE TABLE IF NOT EXISTS knowledge_doc (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_name VARCHAR(128) COMMENT '如:肺结节诊治中国专家共识2024',
  version VARCHAR(32),
  chunk_count INT,
  status TINYINT COMMENT '1已入库',
  create_by BIGINT, update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指南文档登记';
```

### 步骤 6：gateway 路由追加

```yaml
        - id: ai
          uri: http://localhost:8086
          predicates: [ "Path=/api/ai/**" ]
```

## 5. 验证（全部通过才算完成）

| # | 命令 | 预期 |
|---|---|---|
| 1 | `mvn install` | 16 模块全 SUCCESS，新增 24 个测试（ai 20：敏感 5 + 抽取 5 + RAG 6 + 工具 4；nodule 4）全绿 |
| 2 | 脱敏证据 | `promptIsSanitizedBeforeSend` 通过：出网文本不含原始手机号 |
| 3 | 越权防护证据 | `queryNextFollowupUsesCurrentUserNotLlmSupplied` 通过：LLM 无法注入 patientId |
| 4 | 确认链路证据 | `confirmCreatesNoduleSnapshotAndPlan` 通过：状态 1→2 + 建结节快照 + 排期调用 |
| 5 | 免责语强制 | 解读/问答 SSE 流的最后一组 delta 必含"辅助参考"（服务端追加，非 LLM 输出） |

## 6. 输出要求

结果摘要（1-3 句）→ 详细变更（文件树 + 关键决策）→ 验证结果（逐条命令输出摘要）→ 风险提醒（红区：PII 出网过滤、输出免责、工具越权防护，需人工逐行审查）→ 未决问题（P4b 清单：Seata、PgVector、会话记忆、Rerank）。

## 7. 禁止事项

- 禁止 AI 抽取结果未经确认直接入库（"AI 只起草、人确认"底线）
- 禁止脱敏后的敏感文本出网（残留即终止）
- 禁止工具方法接受 LLM 提供的 patientId 参数
- 禁止把 LLM API Key 明文写入仓库（必须环境变量占位）
- 禁止用 LLM 输出替代免责语（服务端强制追加）
- 禁止跳过验证直接报告完成
