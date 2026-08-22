package com.yiliao.patient.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.time.LocalDate;

/** patient_diagnosis：随访模板匹配输入（specs/modules/patient.md §2）。 */
@TableName("patient_diagnosis")
public class PatientDiagnosis extends BaseEntity {
    private Long patientId;
    private String tnmStage;
    private String clinicalStage;
    private String pathologyType;
    private String geneResult;
    private LocalDate surgeryDate;
    private Integer adjuvantTherapy;
    private LocalDate diagnoseDate;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getTnmStage() { return tnmStage; }
    public void setTnmStage(String tnmStage) { this.tnmStage = tnmStage; }
    public String getClinicalStage() { return clinicalStage; }
    public void setClinicalStage(String clinicalStage) { this.clinicalStage = clinicalStage; }
    public String getPathologyType() { return pathologyType; }
    public void setPathologyType(String pathologyType) { this.pathologyType = pathologyType; }
    public String getGeneResult() { return geneResult; }
    public void setGeneResult(String geneResult) { this.geneResult = geneResult; }
    public LocalDate getSurgeryDate() { return surgeryDate; }
    public void setSurgeryDate(LocalDate surgeryDate) { this.surgeryDate = surgeryDate; }
    public Integer getAdjuvantTherapy() { return adjuvantTherapy; }
    public void setAdjuvantTherapy(Integer adjuvantTherapy) { this.adjuvantTherapy = adjuvantTherapy; }
    public LocalDate getDiagnoseDate() { return diagnoseDate; }
    public void setDiagnoseDate(LocalDate diagnoseDate) { this.diagnoseDate = diagnoseDate; }
}
