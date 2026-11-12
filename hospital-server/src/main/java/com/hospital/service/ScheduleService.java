package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.po.Schedule;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    IPage<Schedule> pageSchedules(Integer page, Integer size, Integer doctorId,
                                  String department, LocalDate startDate, LocalDate endDate);

    /** 医生端：我的排班 */
    List<Schedule> listByDoctor(Integer doctorId, LocalDate startDate, LocalDate endDate);

    /** 患者端：某医生某天的排班（余号实时取自 Redis） */
    List<Schedule> listByDoctorAndDate(Integer doctorId, LocalDate scheduleDate);

    void addSchedule(Schedule schedule);

    void updateSchedule(Schedule schedule);

    void deleteSchedule(Integer scheduleId);

    /** 把未来排班的号源写入 Redis */
    int warmUpStock();
}
