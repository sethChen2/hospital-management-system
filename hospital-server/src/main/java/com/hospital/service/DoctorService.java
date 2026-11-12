package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.dto.DoctorDTO;
import com.hospital.entity.po.Doctor;
import com.hospital.entity.vo.DoctorExcelVO;

import java.io.InputStream;
import java.util.List;

public interface DoctorService {

    IPage<Doctor> pageDoctors(Integer page, Integer size, String keyword,
                              String department, Integer isOnJob);

    Doctor getDoctor(Integer doctorId);

    /** 按登录用户ID取医生档案（医生端接口用） */
    Doctor getByUserId(Integer userId);

    void addDoctor(DoctorDTO dto);

    void updateDoctor(DoctorDTO dto);

    void deleteDoctor(Integer doctorId);

    /** 导出用数据集 */
    List<DoctorExcelVO> listForExport(String keyword, String department, Integer isOnJob);

    /** 导入：返回 [成功数, 跳过数] */
    int[] importDoctors(InputStream in);

    /** 患者端：按科室查询在职医生 */
    List<Doctor> listByDepartment(String department, String keyword);

    /** 科室列表 */
    List<String> listDepartments();
}
