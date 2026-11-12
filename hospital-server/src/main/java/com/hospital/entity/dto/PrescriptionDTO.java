package com.hospital.entity.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 处方明细行：医生开药时前端动态增删的一行
 */
@Data
public class PrescriptionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择药品")
    private Integer drugId;

    @NotNull(message = "请填写药品数量")
    @Min(value = 1, message = "药品数量至少为 1")
    private Integer drugNum;
}
