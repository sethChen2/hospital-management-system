package com.hospital.service;

import com.hospital.entity.dto.LoginDTO;
import com.hospital.entity.dto.PasswordBackDTO;
import com.hospital.entity.dto.PasswordModifyDTO;
import com.hospital.entity.dto.RegisterPatientDTO;
import com.hospital.entity.dto.UserInfoDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.po.User;
import com.hospital.entity.vo.LoginVO;
import com.hospital.entity.vo.UserInfoVO;

public interface UserService {

    /** 登录，返回 token 与用户信息 */
    LoginVO login(LoginDTO dto);

    /** 患者自助注册 */
    void addPatient(RegisterPatientDTO dto);

    /** 密码找回：账号 + 身份证 */
    void findBackPassword(PasswordBackDTO dto);

    /** 查询当前用户信息 */
    UserInfoVO getInfo(Integer userId);

    /** 修改个人信息 */
    void modifyInfo(Integer userId, UserInfoDTO dto);

    /** 修改密码 */
    void modifyPassword(Integer userId, PasswordModifyDTO dto);

    /* ---------------- 管理员：患者管理 ---------------- */

    IPage<User> pagePatients(Integer page, Integer size, String keyword, Integer accountStatus);

    User getPatient(Integer userId);

    void deletePatient(Integer userId);
}
