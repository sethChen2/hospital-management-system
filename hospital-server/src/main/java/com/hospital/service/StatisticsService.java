package com.hospital.service;

import com.hospital.entity.vo.StatisticsVO;

public interface StatisticsService {

    /** 运营统计汇总 */
    StatisticsVO getStatistics();
}
