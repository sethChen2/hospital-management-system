package com.hospital.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 管理员新增 / 修改医生
 */
@Data
public class DoctorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 修改时必填 */
    private Integer doctorId;

    /** 新增时必填；修改时忽略 */
    private String account;

    /** 新增时必填；修改时忽略 */
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String gender;

    private String phone;

    private String email;

    private String idcard;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birth;

    private String position;

    @NotBlank(message = "科室不能为空")
    private String department;

    private BigDecimal registerFee;

    private Integer isOnJob;

    private Integer accountStatus;
}
