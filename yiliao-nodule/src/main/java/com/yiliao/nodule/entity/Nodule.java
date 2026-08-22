package com.yiliao.nodule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.time.LocalDate;

/** nodule（specs/modules/nodule.md §2）。 */
@TableName("nodule")
public class Nodule extends BaseEntity {
    private Long patientId;
    private String noduleNo;
    private String location;
    private Integer noduleType;
    private Integer multiplicity;
    private Integer status = 1;
    private LocalDate firstFoundDate;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getNoduleNo() { return noduleNo; }
    public void setNoduleNo(String noduleNo) { this.noduleNo = noduleNo; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getNoduleType() { return noduleType; }
    public void setNoduleType(Integer noduleType) { this.noduleType = noduleType; }
    public Integer getMultiplicity() { return multiplicity; }
    public void setMultiplicity(Integer multiplicity) { this.multiplicity = multiplicity; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDate getFirstFoundDate() { return firstFoundDate; }
    public void setFirstFoundDate(LocalDate firstFoundDate) { this.firstFoundDate = firstFoundDate; }
}
