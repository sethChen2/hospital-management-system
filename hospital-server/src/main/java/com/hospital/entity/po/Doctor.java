package com.hospital.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 医生表：user 的 1:1 扩展
 */
@Data
@TableName("doctor")
public class Doctor implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "doctor_id", type = IdType.AUTO)
    private Integer doctorId;

    private Integer userId;

    /** 职称 */
    private String position;

    /** 科室（字符串，不建字典表） */
    private String department;

    /** 挂号费 */
    private BigDecimal registerFee;

    /** 1 在职 0 离职 */
    private Integer isOnJob;

    /* -------- 以下为联表查询返回的 user 字段，不落库 -------- */

    @TableField(exist = false)
    private String account;

    @TableField(exist = false)
    private String name;

    @TableField(exist = false)
    private String gender;

    @TableField(exist = false)
    private String phone;

    @TableField(exist = false)
    private String email;

    @TableField(exist = false)
    private String idcard;

    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birth;

    @TableField(exist = false)
    private Integer accountStatus;
}
