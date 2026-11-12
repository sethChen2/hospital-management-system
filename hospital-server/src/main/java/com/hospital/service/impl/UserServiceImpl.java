package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.Constants;
import com.hospital.common.exception.BusinessException;
import com.hospital.entity.dto.LoginDTO;
import com.hospital.entity.dto.PasswordBackDTO;
import com.hospital.entity.dto.PasswordModifyDTO;
import com.hospital.entity.dto.RegisterPatientDTO;
import com.hospital.entity.dto.UserInfoDTO;
import com.hospital.entity.po.Bed;
import com.hospital.entity.po.Doctor;
import com.hospital.entity.po.RegisterRecord;
import com.hospital.entity.po.User;
import com.hospital.entity.vo.LoginVO;
import com.hospital.entity.vo.UserInfoVO;
import com.hospital.mapper.BedMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.RegisterRecordMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.service.UserService;
import com.hospital.utils.JwtUtil;
import com.hospital.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;
    private final RegisterRecordMapper registerRecordMapper;
    private final BedMapper bedMapper;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserMapper userMapper, DoctorMapper doctorMapper,
                           RegisterRecordMapper registerRecordMapper, BedMapper bedMapper,
                           JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.doctorMapper = doctorMapper;
        this.registerRecordMapper = registerRecordMapper;
        this.bedMapper = bedMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getAccount, dto.getAccount()));
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (user.getAccountStatus() != null && user.getAccountStatus() == 0) {
            throw new BusinessException("账号已被停用，请联系管理员");
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        String token = jwtUtil.createToken(user.getUserId(), user.getAccount(), user.getRole());
        LoginVO vo = new LoginVO();
        fillUserInfo(vo, user);
        vo.setToken(token);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPatient(RegisterPatientDTO dto) {
        Long sameAccount = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getAccount, dto.getAccount()));
        if (sameAccount != null && sameAccount > 0) {
            throw new BusinessException("该账号已被注册");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setRole(Constants.ROLE_PATIENT);
        user.setAccountStatus(1);
        userMapper.insert(user);
        log.info("患者注册成功：account={}, userId={}", user.getAccount(), user.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void findBackPassword(PasswordBackDTO dto) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getAccount, dto.getAccount()));
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (user.getIdcard() == null || !user.getIdcard().equalsIgnoreCase(dto.getIdcard())) {
            throw new BusinessException("身份证号与账号不匹配");
        }
        User update = new User();
        update.setUserId(user.getUserId());
        update.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        userMapper.updateById(update);
        log.info("密码找回成功：account={}", user.getAccount());
    }

    @Override
    public UserInfoVO getInfo(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserInfoVO vo = new UserInfoVO();
        fillUserInfo(vo, user);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyInfo(Integer userId, UserInfoDTO dto) {
        User update = new User();
        update.setUserId(userId);
        update.setName(dto.getName());
        update.setGender(dto.getGender());
        update.setPhone(dto.getPhone());
        update.setEmail(dto.getEmail());
        update.setIdcard(dto.getIdcard());
        update.setBirth(dto.getBirth());
        userMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyPassword(Integer userId, PasswordModifyDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!PasswordUtil.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        User update = new User();
        update.setUserId(userId);
        update.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    @Override
    public IPage<User> pagePatients(Integer page, Integer size, String keyword, Integer accountStatus) {
        return userMapper.selectPage(new Page<>(page, size), Wrappers.<User>lambdaQuery()
                .eq(User::getRole, Constants.ROLE_PATIENT)
                .eq(accountStatus != null, User::getAccountStatus, accountStatus)
                .and(org.springframework.util.StringUtils.hasText(keyword), w -> w
                        .like(User::getName, keyword)
                        .or().like(User::getAccount, keyword)
                        .or().like(User::getPhone, keyword))
                .orderByAsc(User::getUserId));
    }

    @Override
    public User getPatient(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !Constants.ROLE_PATIENT.equals(user.getRole())) {
            throw new BusinessException("患者不存在");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePatient(Integer userId) {
        getPatient(userId);
        Long registers = registerRecordMapper.selectCount(Wrappers.<RegisterRecord>lambdaQuery()
                .eq(RegisterRecord::getPatientUserId, userId));
        if (registers != null && registers > 0) {
            throw new BusinessException("该患者已有 " + registers + " 条挂号记录，无法删除");
        }
        Long beds = bedMapper.selectCount(Wrappers.<Bed>lambdaQuery()
                .eq(Bed::getPatientUserId, userId));
        if (beds != null && beds > 0) {
            throw new BusinessException("该患者正在住院，无法删除");
        }
        userMapper.deleteById(userId);
        log.info("删除患者：userId={}", userId);
    }

    /** 公共字段填充：医生角色额外带出 doctorId / 科室 / 职称 / 挂号费 */
    private void fillUserInfo(UserInfoVO vo, User user) {
        BeanUtils.copyProperties(user, vo);
        if (Constants.ROLE_DOCTOR.equals(user.getRole())) {
            Doctor doctor = doctorMapper.selectOne(Wrappers.<Doctor>lambdaQuery()
                    .eq(Doctor::getUserId, user.getUserId()));
            if (doctor != null) {
                vo.setDoctorId(doctor.getDoctorId());
                vo.setDepartment(doctor.getDepartment());
                vo.setPosition(doctor.getPosition());
                vo.setRegisterFee(doctor.getRegisterFee());
            }
        }
    }
}
