package com.yiliao.followup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

/** followup_template（specs/modules/followup.md §2）。 */
@TableName("followup_template")
public class FollowupTemplate extends BaseEntity {
    private String templateName;
    private Integer scene;
    private String guidelineSource;
    private String itemsJson;
    private Integer status = 1;

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public Integer getScene() { return scene; }
    public void setScene(Integer scene) { this.scene = scene; }
    public String getGuidelineSource() { return guidelineSource; }
    public void setGuidelineSource(String guidelineSource) { this.guidelineSource = guidelineSource; }
    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
