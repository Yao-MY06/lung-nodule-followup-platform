package com.yiliao.patient.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.math.BigDecimal;

/** patient_risk_factor：直接影响随访策略选择（specs/modules/patient.md §2）。 */
@TableName("patient_risk_factor")
public class PatientRiskFactor extends BaseEntity {
    private Long patientId;
    private BigDecimal smokingPackYear;
    private Integer familyHistory;
    private Integer occupationalExposure;
    private Integer priorCancer;
    private String comorbidity;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public BigDecimal getSmokingPackYear() { return smokingPackYear; }
    public void setSmokingPackYear(BigDecimal smokingPackYear) { this.smokingPackYear = smokingPackYear; }
    public Integer getFamilyHistory() { return familyHistory; }
    public void setFamilyHistory(Integer familyHistory) { this.familyHistory = familyHistory; }
    public Integer getOccupationalExposure() { return occupationalExposure; }
    public void setOccupationalExposure(Integer occupationalExposure) { this.occupationalExposure = occupationalExposure; }
    public Integer getPriorCancer() { return priorCancer; }
    public void setPriorCancer(Integer priorCancer) { this.priorCancer = priorCancer; }
    public String getComorbidity() { return comorbidity; }
    public void setComorbidity(String comorbidity) { this.comorbidity = comorbidity; }

    /** 是否存在任一危险因素（规则引擎 risk 条件输入）。 */
    public boolean hasAnyRiskFactor() {
        return (smokingPackYear != null && smokingPackYear.compareTo(BigDecimal.ZERO) > 0)
                || Integer.valueOf(1).equals(familyHistory)
                || Integer.valueOf(1).equals(occupationalExposure)
                || Integer.valueOf(1).equals(priorCancer);
    }
}
