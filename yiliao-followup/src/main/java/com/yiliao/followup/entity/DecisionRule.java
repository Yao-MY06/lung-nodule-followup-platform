package com.yiliao.followup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

/** decision_rule：表驱动规则（specs/modules/followup.md §4，禁止硬编码间隔）。 */
@TableName("decision_rule")
public class DecisionRule extends BaseEntity {
    private Integer scene;
    private String condJson;
    private Long resultTemplateId;
    private Integer firstIntervalMonth;
    private Integer repeatIntervalMonth;
    private Integer totalYears;
    private Integer priority = 0;
    private Integer status = 1;

    public Integer getScene() { return scene; }
    public void setScene(Integer scene) { this.scene = scene; }
    public String getCondJson() { return condJson; }
    public void setCondJson(String condJson) { this.condJson = condJson; }
    public Long getResultTemplateId() { return resultTemplateId; }
    public void setResultTemplateId(Long resultTemplateId) { this.resultTemplateId = resultTemplateId; }
    public Integer getFirstIntervalMonth() { return firstIntervalMonth; }
    public void setFirstIntervalMonth(Integer firstIntervalMonth) { this.firstIntervalMonth = firstIntervalMonth; }
    public Integer getRepeatIntervalMonth() { return repeatIntervalMonth; }
    public void setRepeatIntervalMonth(Integer repeatIntervalMonth) { this.repeatIntervalMonth = repeatIntervalMonth; }
    public Integer getTotalYears() { return totalYears; }
    public void setTotalYears(Integer totalYears) { this.totalYears = totalYears; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
