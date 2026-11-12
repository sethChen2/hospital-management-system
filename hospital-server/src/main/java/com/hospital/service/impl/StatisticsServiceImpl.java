package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hospital.common.Constants;
import com.hospital.entity.po.User;
import com.hospital.entity.vo.StatisticsVO;
import com.hospital.mapper.BedMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.RegisterRecordMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final int TREND_DAYS = 7;
    private static final int DOCTOR_TOP = 10;

    private final RegisterRecordMapper registerRecordMapper;
    private final BedMapper bedMapper;
    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;

    public StatisticsServiceImpl(RegisterRecordMapper registerRecordMapper, BedMapper bedMapper,
                                 UserMapper userMapper, DoctorMapper doctorMapper) {
        this.registerRecordMapper = registerRecordMapper;
        this.bedMapper = bedMapper;
        this.userMapper = userMapper;
        this.doctorMapper = doctorMapper;
    }

    @Override
    public StatisticsVO getStatistics() {
        StatisticsVO vo = new StatisticsVO();
        vo.setDepartmentRegisters(registerRecordMapper.countByDepartment());
        vo.setGenderDistribution(registerRecordMapper.countByGender());
        vo.setRegisterTrend(registerRecordMapper.countByDate(TREND_DAYS));
        vo.setDepartmentIncome(registerRecordMapper.sumIncomeByDepartment());
        vo.setDoctorWorkload(registerRecordMapper.countByDoctor(DOCTOR_TOP));
        vo.setBedStatus(bedMapper.countByStatus());

        vo.setTotalRegisters(registerRecordMapper.selectCount(null));
        vo.setTotalPatients(userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getRole, Constants.ROLE_PATIENT)));
        vo.setTotalDoctors(doctorMapper.selectCount(null));
        vo.setTotalIncome(sumIncome(vo.getDepartmentIncome()));

        int used = countOf(vo.getBedStatus(), Constants.BED_USED);
        int free = countOf(vo.getBedStatus(), Constants.BED_FREE);
        int total = used + free;
        vo.setBedTotal(total);
        vo.setBedUsed(used);
        vo.setBedFree(free);
        vo.setBedUsageRate(total == 0 ? 0.0 : Math.round(used * 1000.0 / total) / 10.0);
        return vo;
    }

    private BigDecimal sumIncome(List<Map<String, Object>> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        if (rows == null) {
            return sum;
        }
        for (Map<String, Object> row : rows) {
            Object value = row.get("value");
            if (value != null) {
                sum = sum.add(new BigDecimal(value.toString()));
            }
        }
        return sum;
    }

    private int countOf(List<Map<String, Object>> rows, String name) {
        if (rows == null) {
            return 0;
        }
        for (Map<String, Object> row : rows) {
            if (name.equals(String.valueOf(row.get("name")))) {
                return ((Number) row.get("value")).intValue();
            }
        }
        return 0;
    }
}
