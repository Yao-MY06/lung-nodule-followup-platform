package com.yiliao.patient.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.time.LocalDate;

/** patient_archive：id_card 加密存储（IdCardCipher），出参脱敏（specs/modules/patient.md §2/§4.3）。 */
@TableName("patient_archive")
public class PatientArchive extends BaseEntity {

    public static final String STAGE_NODULE = "结节随访中";
    public static final String STAGE_POSTOP = "术后随访中";
    public static final String STAGE_TREATING = "治疗中";
    public static final String STAGE_MDT = "MDT中";
    public static final String STAGE_LOST = "失访";
    public static final String STAGE_CLOSED = "结案";

    private String patientNo;
    private Long userId;
    private String name;
    private Integer gender;
    private LocalDate birthDate;
    private String idCard;
    private String phone;
    private String address;
    private String emergencyContact;
    private String emergencyPhone;
    private Long doctorId;
    private String stageLabel;
    private Integer sourceType;
    private String remark;

    public String getPatientNo() { return patientNo; }
    public void setPatientNo(String patientNo) { this.patientNo = patientNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getStageLabel() { return stageLabel; }
    public void setStageLabel(String stageLabel) { this.stageLabel = stageLabel; }
    public Integer getSourceType() { return sourceType; }
    public void setSourceType(Integer sourceType) { this.sourceType = sourceType; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
