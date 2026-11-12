package com.hospital.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 运营统计：管理员首页与统计页共用
 */
@Data
public class StatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总挂号量 */
    private long totalRegisters;

    /** 总患者数 */
    private long totalPatients;

    /** 总医生数 */
    private long totalDoctors;

    /** 已缴费收入合计 */
    private BigDecimal totalIncome;

    /* -------- 床位 -------- */

    private int bedTotal;

    private int bedUsed;

    private int bedFree;

    /** 床位使用率（百分比，保留一位小数） */
    private double bedUsageRate;

    /* -------- 图表数据：每项都是 [{name, value}] -------- */

    /** 各科室挂号量 */
    private List<Map<String, Object>> departmentRegisters;

    /** 患者性别分布 */
    private List<Map<String, Object>> genderDistribution;

    /** 近 7 天挂号趋势 */
    private List<Map<String, Object>> registerTrend;

    /** 各科室收入 */
    private List<Map<String, Object>> departmentIncome;

    /** 医生工作量 Top 10 */
    private List<Map<String, Object>> doctorWorkload;

    /** 床位状态分布 */
    private List<Map<String, Object>> bedStatus;
}
