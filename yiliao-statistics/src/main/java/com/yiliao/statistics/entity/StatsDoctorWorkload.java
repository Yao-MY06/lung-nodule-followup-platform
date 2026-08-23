package com.yiliao.statistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.time.LocalDate;

/** stats_doctor_workload：医生工作量日快照（specs/modules/statistics.md §2，聚合维度非 PII 明细）。 */
@TableName("stats_doctor_workload")
public class StatsDoctorWorkload extends BaseEntity {
    private LocalDate statDate;
    private Long doctorId;
    private String doctorName;
    private Integer archiveCount;
    private Integer doneCount;
    private Integer alertCount;

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public Integer getArchiveCount() { return archiveCount; }
    public void setArchiveCount(Integer archiveCount) { this.archiveCount = archiveCount; }
    public Integer getDoneCount() { return doneCount; }
    public void setDoneCount(Integer doneCount) { this.doneCount = doneCount; }
    public Integer getAlertCount() { return alertCount; }
    public void setAlertCount(Integer alertCount) { this.alertCount = alertCount; }
}
