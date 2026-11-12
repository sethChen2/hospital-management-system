package com.hospital.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 检查单表
 */
@Data
@TableName("registercheck")
public class RegisterCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "rc_id", type = IdType.AUTO)
    private Integer rcId;

    private Integer registerId;

    private Integer checkId;

    /* -------- 联表查询字段 -------- */

    @TableField(exist = false)
    private String checkName;

    @TableField(exist = false)
    private BigDecimal price;
}
