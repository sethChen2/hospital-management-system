package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.po.RegisterRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface RegisterRecordMapper extends BaseMapper<RegisterRecord> {

    /** 通用分页查询挂号单（管理员 / 医生 / 患者共用） */
    IPage<RegisterRecord> pageRecords(IPage<RegisterRecord> page,
                                      @Param("patientUserId") Integer patientUserId,
                                      @Param("doctorId") Integer doctorId,
                                      @Param("registerStatus") String registerStatus,
                                      @Param("payStatus") String payStatus,
                                      @Param("keyword") String keyword);

    /** 查询单条挂号单详情 */
    RegisterRecord selectDetail(@Param("registerId") Integer registerId);

    /** 医生历史就诊记录 */
    List<RegisterRecord> selectHistoryByDoctor(@Param("doctorId") Integer doctorId,
                                               @Param("patientUserId") Integer patientUserId);

    /* ---------------- 运营统计 ---------------- */

    /** 各科室挂号量 */
    List<Map<String, Object>> countByDepartment();

    /** 患者性别分布 */
    List<Map<String, Object>> countByGender();

    /** 近 N 天挂号趋势 */
    List<Map<String, Object>> countByDate(@Param("days") Integer days);

    /** 各科室收入 */
    List<Map<String, Object>> sumIncomeByDepartment();

    /** 医生工作量 Top N */
    List<Map<String, Object>> countByDoctor(@Param("limit") Integer limit);
}
