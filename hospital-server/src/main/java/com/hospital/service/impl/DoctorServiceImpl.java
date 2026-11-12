package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.Constants;
import com.hospital.common.exception.BusinessException;
import com.hospital.entity.dto.DoctorDTO;
import com.hospital.entity.po.Bed;
import com.hospital.entity.po.Doctor;
import com.hospital.entity.po.RegisterRecord;
import com.hospital.entity.po.Schedule;
import com.hospital.entity.po.User;
import com.hospital.entity.vo.DoctorExcelVO;
import com.hospital.mapper.BedMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.RegisterRecordMapper;
import com.hospital.mapper.ScheduleMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.service.DoctorService;
import com.hospital.utils.ExcelUtil;
import com.hospital.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DoctorServiceImpl implements DoctorService {

    /** 医生账号默认初始化密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final RegisterRecordMapper registerRecordMapper;
    private final ScheduleMapper scheduleMapper;
    private final BedMapper bedMapper;

    public DoctorServiceImpl(DoctorMapper doctorMapper, UserMapper userMapper,
                             RegisterRecordMapper registerRecordMapper,
                             ScheduleMapper scheduleMapper, BedMapper bedMapper) {
        this.doctorMapper = doctorMapper;
        this.userMapper = userMapper;
        this.registerRecordMapper = registerRecordMapper;
        this.scheduleMapper = scheduleMapper;
        this.bedMapper = bedMapper;
    }

    @Override
    public IPage<Doctor> pageDoctors(Integer page, Integer size, String keyword,
                                     String department, Integer isOnJob) {
        return doctorMapper.pageDoctors(new Page<>(page, size), keyword, department, isOnJob);
    }

    @Override
    public Doctor getDoctor(Integer doctorId) {
        Doctor doctor = doctorMapper.selectDetail(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }
        return doctor;
    }

    @Override
    public Doctor getByUserId(Integer userId) {
        Doctor doctor = doctorMapper.selectOne(Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getUserId, userId));
        if (doctor == null) {
            throw new BusinessException("医生档案不存在");
        }
        return doctor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDoctor(DoctorDTO dto) {
        if (!StringUtils.hasText(dto.getAccount())) {
            throw new BusinessException("账号不能为空");
        }
        Long exist = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getAccount, dto.getAccount()));
        if (exist != null && exist > 0) {
            throw new BusinessException("账号已存在");
        }

        User user = new User();
        user.setAccount(dto.getAccount());
        user.setPassword(PasswordUtil.encode(
                StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : DEFAULT_PASSWORD));
        user.setName(dto.getName());
        user.setGender(StringUtils.hasText(dto.getGender()) ? dto.getGender() : "男");
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setIdcard(dto.getIdcard());
        user.setBirth(dto.getBirth());
        user.setRole(Constants.ROLE_DOCTOR);
        user.setAccountStatus(dto.getAccountStatus() == null ? 1 : dto.getAccountStatus());
        userMapper.insert(user);

        Doctor doctor = new Doctor();
        doctor.setUserId(user.getUserId());
        doctor.setPosition(dto.getPosition());
        doctor.setDepartment(dto.getDepartment());
        doctor.setRegisterFee(dto.getRegisterFee() == null ? BigDecimal.ZERO : dto.getRegisterFee());
        doctor.setIsOnJob(dto.getIsOnJob() == null ? 1 : dto.getIsOnJob());
        doctorMapper.insert(doctor);
        log.info("新增医生成功：doctorId={}, account={}", doctor.getDoctorId(), user.getAccount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDoctor(DoctorDTO dto) {
        if (dto.getDoctorId() == null) {
            throw new BusinessException("医生ID不能为空");
        }
        Doctor exists = doctorMapper.selectById(dto.getDoctorId());
        if (exists == null) {
            throw new BusinessException("医生不存在");
        }

        User user = new User();
        user.setUserId(exists.getUserId());
        user.setName(dto.getName());
        user.setGender(dto.getGender());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setIdcard(dto.getIdcard());
        user.setBirth(dto.getBirth());
        user.setAccountStatus(dto.getAccountStatus());
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(PasswordUtil.encode(dto.getPassword()));
        }
        userMapper.updateById(user);

        Doctor doctor = new Doctor();
        doctor.setDoctorId(dto.getDoctorId());
        doctor.setPosition(dto.getPosition());
        doctor.setDepartment(dto.getDepartment());
        doctor.setRegisterFee(dto.getRegisterFee());
        doctor.setIsOnJob(dto.getIsOnJob());
        doctorMapper.updateById(doctor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDoctor(Integer doctorId) {
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }
        Long registers = registerRecordMapper.selectCount(Wrappers.<RegisterRecord>lambdaQuery()
                .eq(RegisterRecord::getDoctorId, doctorId));
        if (registers != null && registers > 0) {
            throw new BusinessException("该医生已有 " + registers + " 条挂号记录，无法删除，可将在职状态改为离职");
        }
        Long hospitalBeds = bedMapper.selectCount(Wrappers.<Bed>lambdaQuery()
                .eq(Bed::getDoctorId, doctorId));
        if (hospitalBeds != null && hospitalBeds > 0) {
            throw new BusinessException("该医生名下还有住院患者，无法删除");
        }
        // 无挂号记录后，其排班可安全清理，否则外键会拦截删除
        scheduleMapper.delete(Wrappers.<Schedule>lambdaQuery()
                .eq(Schedule::getDoctorId, doctorId));
        doctorMapper.deleteById(doctorId);
        userMapper.deleteById(doctor.getUserId());
        log.info("删除医生成功：doctorId={}", doctorId);
    }

    @Override
    public List<DoctorExcelVO> listForExport(String keyword, String department, Integer isOnJob) {
        IPage<Doctor> page = doctorMapper.pageDoctors(new Page<>(1, 1000), keyword, department, isOnJob);
        List<DoctorExcelVO> list = new ArrayList<>();
        for (Doctor d : page.getRecords()) {
            DoctorExcelVO vo = new DoctorExcelVO();
            BeanUtils.copyProperties(d, vo);
            list.add(vo);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int[] importDoctors(InputStream in) {
        List<DoctorExcelVO> rows;
        try {
            rows = ExcelUtil.importExcel(in, DoctorExcelVO.class);
        } catch (Exception e) {
            log.error("医生 Excel 解析失败", e);
            throw new BusinessException("Excel 解析失败，请检查文件格式");
        }
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("Excel 中没有可导入的数据");
        }

        int success = 0;
        int skip = 0;
        for (DoctorExcelVO row : rows) {
            if (!StringUtils.hasText(row.getAccount()) || !StringUtils.hasText(row.getName())
                    || !StringUtils.hasText(row.getDepartment())) {
                skip++;
                continue;
            }
            Long exist = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(User::getAccount, row.getAccount().trim()));
            if (exist != null && exist > 0) {
                skip++;
                continue;
            }
            DoctorDTO dto = new DoctorDTO();
            BeanUtils.copyProperties(row, dto);
            dto.setAccount(row.getAccount().trim());
            dto.setIsOnJob(row.getIsOnJob() == null ? 1 : row.getIsOnJob());
            addDoctor(dto);
            success++;
        }
        log.info("医生 Excel 导入完成：成功 {} 条，跳过 {} 条", success, skip);
        return new int[]{success, skip};
    }

    @Override
    public List<Doctor> listByDepartment(String department, String keyword) {
        return doctorMapper.selectListDetail(department, keyword);
    }

    @Override
    public List<String> listDepartments() {
        return doctorMapper.selectDepartments();
    }
}
