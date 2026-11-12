package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.po.Doctor;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DoctorMapper extends BaseMapper<Doctor> {

    /** 分页查询医生（联表带出 user 信息） */
    IPage<Doctor> pageDoctors(IPage<Doctor> page,
                              @Param("keyword") String keyword,
                              @Param("department") String department,
                              @Param("isOnJob") Integer isOnJob);

    /** 查询医生完整信息 */
    Doctor selectDetail(@Param("doctorId") Integer doctorId);

    /** 按科室/关键字查询在职医生列表（患者端选医生） */
    List<Doctor> selectListDetail(@Param("department") String department,
                                  @Param("keyword") String keyword);

    /** 科室列表（doctor.department 去重） */
    List<String> selectDepartments();
}
