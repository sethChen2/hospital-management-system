package com.hospital.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 用户修改个人信息（只允许改这些字段）
 */
@Data
public class UserInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private String gender;

    private String phone;

    private String email;

    private String idcard;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birth;
}
