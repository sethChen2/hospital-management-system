package com.hospital.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 医生申请住院：为该挂号单的患者分配一张空闲病床
 */
@Data
public class BedApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "挂号单不能为空")
    private Integer registerId;

    /** 住院原因 / 病情说明 */
    private String hospitalReason;
}
