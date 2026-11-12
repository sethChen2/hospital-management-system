package com.hospital.controller;

import com.hospital.common.Constants;
import com.hospital.common.R;
import com.hospital.entity.dto.LoginDTO;
import com.hospital.entity.dto.PasswordBackDTO;
import com.hospital.entity.dto.PasswordModifyDTO;
import com.hospital.entity.dto.RegisterPatientDTO;
import com.hospital.entity.dto.UserInfoDTO;
import com.hospital.entity.vo.LoginVO;
import com.hospital.entity.vo.UserInfoVO;
import com.hospital.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户 / 认证 控制层
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 探针接口 */
    @GetMapping("/ping")
    public R<String> ping() {
        return R.ok("pong");
    }

    /** 登录（公开） */
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return R.ok("登录成功", userService.login(dto));
    }

    /** 患者注册（公开） */
    @PostMapping("/addPatient")
    public R<Void> addPatient(@RequestBody @Valid RegisterPatientDTO dto) {
        userService.addPatient(dto);
        return R.ok("注册成功", null);
    }

    /** 密码找回（公开）：账号 + 身份证 */
    @PostMapping("/findBackPassword")
    public R<Void> findBackPassword(@RequestBody @Valid PasswordBackDTO dto) {
        userService.findBackPassword(dto);
        return R.ok("密码重置成功", null);
    }

    /** 当前登录用户信息 */
    @GetMapping("/getInfo")
    public R<UserInfoVO> getInfo(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId) {
        return R.ok(userService.getInfo(userId));
    }

    /** 修改个人信息 */
    @PostMapping("/modifyInfo")
    public R<Void> modifyInfo(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                              @RequestBody UserInfoDTO dto) {
        userService.modifyInfo(userId, dto);
        return R.ok("修改成功", null);
    }

    /** 修改密码 */
    @PostMapping("/modifyPassword")
    public R<Void> modifyPassword(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                                  @RequestBody @Valid PasswordModifyDTO dto) {
        userService.modifyPassword(userId, dto);
        return R.ok("密码修改成功，请重新登录", null);
    }

    /** 退出登录（JWT 无状态，前端清除本地 token 即可） */
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok("已退出登录", null);
    }
}
