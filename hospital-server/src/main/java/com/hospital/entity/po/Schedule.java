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

/**
 * 排班表：号源存 Redis，remain_num 为数据库兜底
 */
@Data
@TableName("schedule")
public class Schedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "schedule_id", type = IdType.AUTO)
    private Integer scheduleId;

    private Integer doctorId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /** AM / PM */
    private String timePeriod;

    private Integer maxNum;

    private Integer remainNum;

    @Version
    private Integer version;

    /* -------- 联表查询字段 -------- */

    @TableField(exist = false)
    private String doctorName;

    @TableField(exist = false)
    private String department;

    @TableField(exist = false)
    private String position;

    @TableField(exist = false)
    private BigDecimal registerFee;
}
