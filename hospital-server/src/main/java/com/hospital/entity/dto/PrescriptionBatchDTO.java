package com.hospital.entity.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 批量开处方：一次提交多行，服务端统一算小计并合计费用
 */
@Data
public class PrescriptionBatchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "挂号单不能为空")
    private Integer registerId;

    @NotEmpty(message = "请至少添加一种药品")
    @Valid
    private List<PrescriptionDTO> prescriptions;
}
