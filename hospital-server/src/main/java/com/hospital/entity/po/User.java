package com.hospital.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 用户表：管理员 / 医生 / 患者的公共账号
 */
@Data
@TableName("`user`")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer userId;

    /** 登录账号 */
    private String account;

    /** BCrypt 密码，任何情况下都不向前端输出 */
    @JsonIgnore
    private String password;

    private String name;

    private String gender;

    private String phone;

    private String email;

    private String idcard;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birth;

    /** ADMIN / DOCTOR / PATIENT */
    private String role;

    /** 1 正常 0 禁用 */
    private Integer accountStatus;
}
