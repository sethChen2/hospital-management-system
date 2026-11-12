package com.hospital.entity.vo;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 医生 Excel 导入导出模型
 */
@Data
public class DoctorExcelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "账号", width = 16, orderNum = "1")
    private String account;

    @Excel(name = "姓名", width = 12, orderNum = "2")
    private String name;

    @Excel(name = "性别", width = 8, orderNum = "3")
    private String gender;

    @Excel(name = "手机号", width = 15, orderNum = "4")
    private String phone;

    @Excel(name = "邮箱", width = 22, orderNum = "5")
    private String email;

    @Excel(name = "身份证号", width = 22, orderNum = "6")
    private String idcard;

    @Excel(name = "职称", width = 14, orderNum = "7")
    private String position;

    @Excel(name = "科室", width = 12, orderNum = "8")
    private String department;

    @Excel(name = "挂号费", width = 10, orderNum = "9", type = 10)
    private BigDecimal registerFee;

    @Excel(name = "在职状态", width = 10, orderNum = "10", replace = {"在职_1", "离职_0"})
    private Integer isOnJob;
}
