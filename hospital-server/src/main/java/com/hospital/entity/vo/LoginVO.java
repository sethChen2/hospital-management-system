package com.hospital.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录结果：用户信息 + token
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginVO extends UserInfoVO {

    private static final long serialVersionUID = 1L;

    private String token;
}
