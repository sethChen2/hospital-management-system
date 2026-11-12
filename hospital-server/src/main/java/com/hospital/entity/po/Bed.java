package com.hospital.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 病床表
 */
@Data
@TableName("bed")
public class Bed implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "bed_id", type = IdType.AUTO)
    private Integer bedId;

    private Integer patientUserId;

    private Integer doctorId;

    /** 空闲 / 占用 */
    private String bedStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime hospitalTime;

    private String hospitalReason;

    @Version
    private Integer version;

    /* -------- 联表查询字段 -------- */

    @TableField(exist = false)
    private String patientName;

    @TableField(exist = false)
    private String doctorName;

    @TableField(exist = false)
    private String department;
}
