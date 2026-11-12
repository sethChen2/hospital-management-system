package com.hospital.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 批量开检查单
 */
@Data
public class CheckBatchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "挂号单不能为空")
    private Integer registerId;

    @NotEmpty(message = "请至少选择一项检查")
    private List<Integer> checkIds;
}
