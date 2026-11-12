package com.hospital.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 书写病历
 */
@Data
public class DiagnosisDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "挂号单不能为空")
    private Integer registerId;

    @NotBlank(message = "请填写诊断结论")
    private String diagnosis;
}
