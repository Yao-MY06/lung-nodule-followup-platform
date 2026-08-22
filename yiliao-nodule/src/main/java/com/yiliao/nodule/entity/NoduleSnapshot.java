package com.yiliao.nodule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** nodule_snapshot：对比核心字段 max_diameter_mm / solid_diameter_mm（specs/modules/nodule.md §2）。 */
@TableName("nodule_snapshot")
public class NoduleSnapshot extends BaseEntity {
    private Long noduleId;
    private Long reportId;
    private LocalDate examDate;
    private BigDecimal maxDiameterMm;
    private BigDecimal solidDiameterMm;
    private Integer meanDensityHu;
    private String signs;
    private BigDecimal volumeMm3;
    private Integer isNew = 0;

    public Long getNoduleId() { return noduleId; }
    public void setNoduleId(Long noduleId) { this.noduleId = noduleId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
    public BigDecimal getMaxDiameterMm() { return maxDiameterMm; }
    public void setMaxDiameterMm(BigDecimal maxDiameterMm) { this.maxDiameterMm = maxDiameterMm; }
    public BigDecimal getSolidDiameterMm() { return solidDiameterMm; }
    public void setSolidDiameterMm(BigDecimal solidDiameterMm) { this.solidDiameterMm = solidDiameterMm; }
    public Integer getMeanDensityHu() { return meanDensityHu; }
    public void setMeanDensityHu(Integer meanDensityHu) { this.meanDensityHu = meanDensityHu; }
    public String getSigns() { return signs; }
    public void setSigns(String signs) { this.signs = signs; }
    public BigDecimal getVolumeMm3() { return volumeMm3; }
    public void setVolumeMm3(BigDecimal volumeMm3) { this.volumeMm3 = volumeMm3; }
    public Integer getIsNew() { return isNew; }
    public void setIsNew(Integer isNew) { this.isNew = isNew; }
}
