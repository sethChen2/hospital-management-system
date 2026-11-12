package com.hospital.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 挂号记录表
 */
@Data
@TableName("registerrecord")
public class RegisterRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "register_id", type = IdType.AUTO)
    private Integer registerId;

    private Integer patientUserId;

    private Integer doctorId;

    private Integer scheduleId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appointTime;

    /** 待就诊 / 就诊中 / 已完成 / 已取消 */
    private String registerStatus;

    private String diagnosis;

    private String symptom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private BigDecimal totalCost;

    /** 未缴费 / 已缴费 */
    private String payStatus;

    @Version
    private Integer version;

    /* -------- 联表查询字段 -------- */

    @TableField(exist = false)
    private String patientName;

    @TableField(exist = false)
    private String patientGender;

    @TableField(exist = false)
    private String patientPhone;

    @TableField(exist = false)
    private String doctorName;

    @TableField(exist = false)
    private String department;

    @TableField(exist = false)
    private String position;

    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    @TableField(exist = false)
    private String timePeriod;

    /** 详情页附带的处方明细 */
    @TableField(exist = false)
    private List<Prescription> prescriptionList;

    /** 详情页附带的检查单 */
    @TableField(exist = false)
    private List<RegisterCheck> checkList;
}
