package com.hospital.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 密码找回：账号 + 身份证校验后重置（不引入 SMTP）
 */
@Data
public class PasswordBackDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "身份证号不能为空")
    private String idcard;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度需为 6-20 位")
    private String newPassword;
}
