package com.yiliao.statistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

/** stats_nodule_dist：结节分布快照，dimension×label 计数（specs/modules/statistics.md §2）。 */
@TableName("stats_nodule_dist")
public class StatsNoduleDist extends BaseEntity {
    /** 维度：nodule_type（结节性质）/ risk_level（风险分层）。 */
    private String dimension;
    /** 标签：实性/部分实性/纯磨玻璃；低危/中危/高危。 */
    private String label;
    private Integer cnt;

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getCnt() { return cnt; }
    public void setCnt(Integer cnt) { this.cnt = cnt; }
}
