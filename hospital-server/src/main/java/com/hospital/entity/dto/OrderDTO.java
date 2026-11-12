package com.hospital.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 患者预约挂号提交
 */
@Data
public class OrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择要预约的排班")
    private Integer scheduleId;

    private String symptom;
}
