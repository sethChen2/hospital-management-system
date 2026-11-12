package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.po.Prescription;
import com.hospital.entity.po.RegisterCheck;
import com.hospital.entity.po.RegisterRecord;

import java.util.List;

public interface RegisterService {

    /** 患者预约挂号，返回挂号单ID */
    Integer addOrder(Integer patientUserId, Integer scheduleId, String symptom);

    IPage<RegisterRecord> pageOrders(Integer page, Integer size, Integer patientUserId,
                                     Integer doctorId, String registerStatus,
                                     String payStatus, String keyword);

    /** 挂号单详情（附带处方明细与检查单） */
    RegisterRecord getOrderDetail(Integer registerId);

    /** 患者取消挂号 */
    void cancelOrder(Integer registerId, Integer patientUserId);

    /** 患者缴纳费用 */
    void payOrder(Integer registerId, Integer patientUserId);

    /** 管理员删除挂号单 */
    void deleteOrder(Integer registerId);

    /* ---------------- 患者端 ---------------- */

    /** 我的处方明细 */
    List<Prescription> findPrescriptions(Integer patientUserId, Integer registerId);

    /** 我的检查单 */
    List<RegisterCheck> findChecks(Integer patientUserId, Integer registerId);

    /** 生成就诊报告单 PDF */
    byte[] buildReportPdf(Integer registerId);

    /** 校验挂号单归属（报告单下载用 token 查询参数做归属校验） */
    void checkReportOwner(Integer patientUserId, Integer registerId);
}
