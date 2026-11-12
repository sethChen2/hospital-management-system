package com.hospital.service;

import com.hospital.entity.dto.PrescriptionDTO;
import com.hospital.entity.po.Prescription;
import com.hospital.entity.po.RegisterCheck;
import com.hospital.entity.po.RegisterRecord;

import java.util.List;

/**
 * 医生诊疗：病历、处方、检查单、住院、结束就诊
 */
public interface DiagnosisService {

    /** 书写/更新病历，并把待就诊置为就诊中 */
    void submitDiagnosis(Integer doctorId, Integer registerId, String diagnosis);

    /** 批量开处方（前端动态增删后一次提交），自动合计费用 */
    void addPrescriptions(Integer doctorId, Integer registerId, List<PrescriptionDTO> prescriptions);

    List<Prescription> findPrescriptions(Integer doctorId, Integer registerId);

    /** 删除一行处方并回退费用 */
    void deletePrescription(Integer doctorId, Integer prescriptionId);

    /** 批量开检查单，自动合计费用 */
    void addRegisterChecks(Integer doctorId, Integer registerId, List<Integer> checkIds);

    List<RegisterCheck> findRegisterChecks(Integer doctorId, Integer registerId);

    /** 申请住院：分配一张空闲床位 */
    void applyBed(Integer doctorId, Integer registerId, String hospitalReason);

    /** 结束就诊：状态置为已完成并记录结束时间 */
    void finishRegister(Integer doctorId, Integer registerId);

    /** 该医生看过的某患者的历史就诊记录 */
    List<RegisterRecord> findHistoryRegisters(Integer doctorId, Integer patientUserId);
}
