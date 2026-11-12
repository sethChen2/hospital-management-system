package com.hospital.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 当前登录用户信息（医生角色附带 doctorId 等档案字段）
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer userId;

    private String account;

    private String name;

    private String gender;

    private String phone;

    private String email;

    private String idcard;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birth;

    private String role;

    private Integer accountStatus;

    /* -------- 医生角色补充字段 -------- */

    private Integer doctorId;

    private String department;

    private String position;

    private BigDecimal registerFee;
}
