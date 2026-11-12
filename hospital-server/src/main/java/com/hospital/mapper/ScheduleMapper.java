package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.po.Schedule;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleMapper extends BaseMapper<Schedule> {

    /** 管理员分页查询排班 */
    IPage<Schedule> pageSchedules(IPage<Schedule> page,
                                  @Param("doctorId") Integer doctorId,
                                  @Param("department") String department,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /** 医生查询本人排班 */
    List<Schedule> selectDetailList(@Param("doctorId") Integer doctorId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /** 患者查询某医生某天排班 */
    List<Schedule> selectDetailByDoctorAndDate(@Param("doctorId") Integer doctorId,
                                               @Param("scheduleDate") LocalDate scheduleDate);

    /**
     * 原子扣减号源：remain_num-1、version+1
     * 用单条 UPDATE 而非「读-改-写」，避免 REPEATABLE READ 快照导致乐观锁重试永远读到旧版本
     * @return 影响行数，0 表示号源已耗尽
     */
    int decreaseStock(@Param("scheduleId") Integer scheduleId);

    /** 原子回补号源：remain_num+1、version+1，且不超过 max_num */
    int increaseStock(@Param("scheduleId") Integer scheduleId);
}
