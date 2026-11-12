package com.hospital.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 药品表
 */
@Data
@TableName("drug")
public class Drug implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "drug_id", type = IdType.AUTO)
    private Integer drugId;

    private String drugName;

    private BigDecimal price;

    private String unit;
}
