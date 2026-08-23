# 分工 E：统计驾驶舱 + AI 问答/解读（SSE）

> 任务书 = 00-通用约定.md + 本文件。允许覆盖的文件（仅此 3 个）：
> `src/views/stats/dashboard.vue`、`src/views/ai/chat.vue`、`src/views/ai/interpret.vue`
> 图表页仅用 `src/api/stats.js`；AI 页用原生 fetch（SSE），不用 axios。

## 1. stats/dashboard.vue

- 顶部 4 个 el-card：`overview()` → managingCount 在管患者 / activePlanCount 进行中计划 / overdueCount 逾期任务（红色，注意字段名以实际返回为准：OverviewVO 含 managingCount/activePlanCount/overdueTaskCount/alertCount/updatedAt）/ alertCount 预警；loading；失败显示 "-"（拦截器已弹错）；卡片底部 updatedAt 小字。
- 2×2 el-row/el-col 栅格 ECharts（各 el-card 内 ref 容器）：
  - 结节类型分布饼图：`noduleDistribution()`（数组 {dimension,label,cnt}，取 dimension==='nodule_type'）。
  - 随访完成率双折线：`followupRate(6)`（{statMonth, completionRate, timelyRate}，0~1 小数）；tooltip 与 y 轴显示百分比（formatter `(v*100).toFixed(0)+'%'`）。
  - 医生工作量柱状：`doctorWorkload()`（{doctorName, archiveCount, doneCount} 双系列）。
  - 第四格：dimension==='risk_level' 的风险分布饼图（无数据时空态）。
- ECharts 生命周期统一封装：init 于 mounted+nextTick、resize 监听、onUnmounted dispose；数据为空数组时仍渲染空坐标轴不报错。

## 2. ai/chat.vue（患者端 AI 助手 /portal/chat）

- 聊天布局：消息区（flex 纵向、overflow auto、用户消息右侧蓝底白字圆角、AI 左侧白底）+ 底部 el-input type=textarea（:rows=2）+ 发送按钮（large）；Enter 发送 / Shift+Enter 换行；发送后自动滚到底（nextTick 设 scrollTop=scrollHeight）。
- 发送流程：
  1. 用户消息入列表；追加 assistant 占位（text=''，thinking=true 显示"思考中…"动画或省略号）。
  2. `fetch('/api/ai/chat', { method:'POST', headers:{'Content-Type':'application/json', Authorization:'Bearer ' + localStorage.getItem('accessToken') }, body: JSON.stringify({ message }) })`。
  3. SSE 解析：`response.body.getReader()` + `TextDecoder('utf-8')` 循环；**buffer 累积拼接**，按 `\n\n` 切事件块，每块解析 `event:` 行与 `data:` 行：delta → assistant.text += data（打字机式追加）；citation → 存 msg.citations；done → 结束。reader done 也结束。
  4. 非 2xx：await response.json() 取 msg 展示；异常兜底文案"AI 服务暂时不可用，请稍后再试"。
- 消息结构 `{ role:'user'|'assistant', text, citations:[] }`（内存数组即可）；citations 在气泡下方灰色小字逐行展示。
- 输入为空/loading 中禁发。

## 3. ai/interpret.vue（报告解读 /portal/interpret）

- 顶部 el-alert info："AI 解读为辅助参考，不替代医生诊断"。
- textarea（rows 8）+【开始解读】按钮（loading 中可"停止"？不做，仅禁用）。
- fetch POST `/api/ai/report/interpret`，body `{ rawText }`，SSE 解析同上（delta/citation/done）。
- 结果区流式渲染：按 `【xxx】` 标题拆段（split 正则 `/(?=【[^】]+】)/`），每段标题加粗；末尾服务端已追加的免责语整体灰色小字。再次点击重新解读（清空重流）。

## 验证自查

SSE 跨 chunk 事件块拼接正确（先 buffer 后 split）；token 读取键名 accessToken；四图表 resize/dispose 成对。
