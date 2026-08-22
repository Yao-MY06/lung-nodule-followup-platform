# references/ —— 参考项目库

> 用途：开发过程中收集的同类架构/领域/技术开源项目，供学习借鉴。
> 维护规则：
> 1. **只学思路，不抄代码**——借鉴任何实现前先看仓库 LICENSE；许可证不明确的项目（如课程配套仓库）仅作设计参考。
> 2. 每次借鉴后在对应文档追加"已借鉴点"记录，标注借鉴到哪个模块/文件。
> 3. 需要本地阅读源码时：`git clone --depth 1 <url> references/repos/<名字>`（repos/ 不纳入工程构建）。
> 4. 新发现的参考项目按类别归档到对应文档，附来源链接与技术栈。

## 分类索引

| 文档 | 方向 | 服务阶段 |
| --- | --- | --- |
| [01-微服务架构参考.md](01-微服务架构参考.md) | Spring Cloud Alibaba 脚手架/电商微服务/开发平台 | P1~P2 已借鉴、P5 治理 |
| [02-医疗随访领域参考.md](02-医疗随访领域参考.md) | HIS/慢病管理/患者随访系统 | 领域建模对照 |
| [03-AI能力参考.md](03-AI能力参考.md) | Spring AI RAG / Tool Calling / Agent | P4 ai-service |

## 快速结论（当前最有价值的三个）

1. **yu-ai-agent**（Spring Boot 3 + Spring AI：RAG/Tool Calling/SSE 全覆盖）——P4 ai-service 的头号参考。
2. **RuoYi-Cloud**（Spring Cloud + RBAC + 网关鉴权成熟范式）——P1 已借鉴的认证模式可对照其实现补菜单/用户管理（P1b）。
3. **MediCareAI**（AI 驱动患者随访与疾病追踪）——与本项目领域最接近，随访工作流设计可对照。
