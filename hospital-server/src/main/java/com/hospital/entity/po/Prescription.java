package com.hospital.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 处方明细表：一张挂号单 1:n 行，每行对应一种药品
 */
@Data
@TableName("prescription")
public class Prescription implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "prescription_id", type = IdType.AUTO)
    private Integer prescriptionId;

    private Integer registerId;

    private Integer drugId;

    private Integer drugNum;

    /** 小计 = 单价 × 数量 */
    private BigDecimal subtotal;

    /* -------- 联表查询字段 -------- */

    @TableField(exist = false)
    private String drugName;

    @TableField(exist = false)
    private BigDecimal price;

    @TableField(exist = false)
    private String unit;
}
