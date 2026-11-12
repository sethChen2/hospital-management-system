package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.po.RegisterCheck;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RegisterCheckMapper extends BaseMapper<RegisterCheck> {

    /** 按挂号单查询检查单（含项目名称与费用） */
    List<RegisterCheck> selectByRegisterId(@Param("registerId") Integer registerId);

    /** 检查费合计，用 SQL SUM 保证 DECIMAL 精度 */
    BigDecimal sumPriceByRegisterId(@Param("registerId") Integer registerId);
}
