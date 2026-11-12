package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.po.Prescription;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PrescriptionMapper extends BaseMapper<Prescription> {

    /** 按挂号单查询处方明细（含药品名称与单价） */
    List<Prescription> selectByRegisterId(@Param("registerId") Integer registerId);

    /** 处方差额合计，用 SQL SUM 保证 DECIMAL 精度 */
    BigDecimal sumSubtotalByRegisterId(@Param("registerId") Integer registerId);
}
