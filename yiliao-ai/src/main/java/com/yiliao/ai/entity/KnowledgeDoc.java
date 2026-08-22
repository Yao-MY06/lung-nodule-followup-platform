package com.yiliao.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

/** knowledge_doc：指南文档登记（切片在服务内存库，P4b 迁 PgVector）。 */
@TableName("knowledge_doc")
public class KnowledgeDoc extends BaseEntity {
    private String docName;
    private String version;
    private Integer chunkCount;
    private Integer status = 1;

    public String getDocName() { return docName; }
    public void setDocName(String docName) { this.docName = docName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
