package com.hospital.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 检查项目表
 */
@Data
@TableName("checkitem")
public class CheckItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "check_id", type = IdType.AUTO)
    private Integer checkId;

    private String checkName;

    private BigDecimal price;
}
