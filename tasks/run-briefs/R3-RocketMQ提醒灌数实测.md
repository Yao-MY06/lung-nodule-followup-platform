# R3：RocketMQ 到点提醒灌数实测（XXL-Job 启用 + 派发 + 幂等）

> 类型：运行态联调（不改业务代码，仅启用配置）　前置：MySQL(3307)/Redis/RocketMQ 运行中；followup+notification+gateway+auth 服务运行中。

## 目标

实测提醒闭环：到期任务 → 每分钟派发 Job → notification 消费发站内信 → 消息落库幂等。这是论文"消息可靠性"素材（specs/flows/F2）。

## 步骤

### S1 启用 XXL-Job

1. 建 xxl_job 库并导入官方表结构：
   ```bash
   curl -sL https://raw.githubusercontent.com/xuxueli/xxl-job/2.4.1/doc/db/tables_xxl_job.sql -o tasks/evidence/R3-tables_xxl_job.sql
   docker exec -i yiliao-mysql sh -c "mysql -uroot -pyiliao123 -e 'CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARACTER SET utf8mb4;'"
   docker cp tasks/evidence/R3-tables_xxl_job.sql yiliao-mysql:/tmp/ && docker exec yiliao-mysql sh -c "mysql -uroot -pyiliao123 xxl_job < /tmp/R3-tables_xxl_job.sql"
   ```
2. 起调度台（compose 文末注释块启用）：`docker compose -f docker-compose.yml -f docker-compose.local.yml up -d xxl-job-admin`（端口 8280）；控制台 http://localhost:8280/xxl-job-admin（admin/123456）。
3. followup 启用执行器后重启：
   ```bash
   mvn -q -f F:\bc\yiliao\pom.xml -pl yiliao-followup spring-boot:run \
     -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://localhost:3307/yiliao_followup?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai --yiliao.xxl-job.enabled=true"
   ```
   控制台注册执行器 appname=yiliao-followup（自动注册），新建任务：JobHandler=remindDispatchJob，cron=`* * * * * ?`（每分钟），启动。

### S2 灌数（让提醒点落在今天）

用医生 Token 经网关建一个计划，其首个任务计划日期=明天（T-1 提醒点=今天）：
- 改 decision_rule id=7 临时：`first_interval_month=0`（首任务=今天，T-7/T-3 已过不派，对账会补）→ 或直接用 SQL 插入一条任务：
```sql
-- 档案1001 的计划任务：计划日期=明天 → 今天命中 T-1 提醒点
INSERT INTO yiliao_followup.followup_plan (patient_id,template_id,rule_id,start_date,end_date,status)
  SELECT 1001,1,7,CURDATE(),DATE_ADD(CURDATE(),INTERVAL 5 YEAR),1 FROM DUAL WHERE NOT EXISTS(SELECT 1 FROM yiliao_followup.followup_plan WHERE patient_id=1001 AND status=1);
INSERT INTO yiliao_followup.followup_task (plan_id,patient_id,seq,plan_date,items_json,status)
  SELECT (SELECT id FROM yiliao_followup.followup_plan WHERE patient_id=1001 AND status=1),
         1001,1,DATE_ADD(CURDATE(),INTERVAL 1 DAY),JSON_OBJECT('items',JSON_ARRAY('胸部CT')),0;
```

### S3 验证派发与幂等

1. 等 ≤2 分钟：`SELECT * FROM yiliao_notify.message_record WHERE biz_key LIKE '%REMIND_T1%' ORDER BY id DESC;` 应出现站内信记录（title="复查提醒（明天）"）。
2. 重复投递实验：用 RocketMQ 控制台或直接对 notification 再发同 bizKey 消费（最简：把 message_record 那条的 Redis 幂等标记删掉再让 Job 重跑？不——正确演示：直接对同一 taskId 手动再投一次同 bizKey 消息（用 RocketMQTemplate 测试端不可行时，删 Redis 键 `yiliao:mq:consumed:YILIAO_REMIND_DUE:{taskId}:REMIND_T1` 后等下一轮）→ 验证 DB 该 biz_key 仍只有 1 行。
3. 对账补投演示：删除该任务的 message_record 并把 remind_sent 置 0，等 hourly 对账 Job（remindReconcileJob）→ 观察补发。

## 验证表

| 检查 | 操作 | 通过标准 |
|---|---|---|
| 派发 | S3-1 | 到点出现"复查提醒（明天）"站内信 |
| 幂等 | S3-2 | biz_key 唯一，重投不重复 |
| 对账补投 | S3-3 | 删除后下一轮自动补发 |
| 消费失败重试 | （可选）notification 杀死后到点恢复 | 消息不丢（MQ 重投） |

## 已知陷阱

- xxl-job-admin 需要 xxl_job 库表先导入；执行器端口 8084 与调度台回调。
- Job 注解的 name 必须和调度台 JobHandler 一致：remindDispatchJob / remindReconcileJob / overdueScanJob。
- 任务完成后把 decision_rule 还原（first_interval_month=6）。

## 输出要求

结果摘要 → 证据（message_record 截图/SQL 输出、调度台执行日志截图存 tasks/evidence/R3-*）→ 未通过项 → 遗留问题。
