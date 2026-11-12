package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.Constants;
import com.hospital.common.exception.BusinessException;
import com.hospital.entity.po.RegisterRecord;
import com.hospital.entity.po.Schedule;
import com.hospital.mapper.RegisterRecordMapper;
import com.hospital.mapper.ScheduleMapper;
import com.hospital.service.ScheduleService;
import com.hospital.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final int DEFAULT_MAX_NUM = 40;

    private final ScheduleMapper scheduleMapper;
    private final RegisterRecordMapper registerRecordMapper;
    private final RedisUtil redisUtil;

    public ScheduleServiceImpl(ScheduleMapper scheduleMapper,
                               RegisterRecordMapper registerRecordMapper,
                               RedisUtil redisUtil) {
        this.scheduleMapper = scheduleMapper;
        this.registerRecordMapper = registerRecordMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public IPage<Schedule> pageSchedules(Integer page, Integer size, Integer doctorId,
                                         String department, LocalDate startDate, LocalDate endDate) {
        return scheduleMapper.pageSchedules(new Page<>(page, size), doctorId, department,
                startDate, endDate);
    }

    @Override
    public List<Schedule> listByDoctor(Integer doctorId, LocalDate startDate, LocalDate endDate) {
        return scheduleMapper.selectDetailList(doctorId, startDate, endDate);
    }

    @Override
    public List<Schedule> listByDoctorAndDate(Integer doctorId, LocalDate scheduleDate) {
        LocalDate date = scheduleDate == null ? LocalDate.now() : scheduleDate;
        List<Schedule> list = scheduleMapper.selectDetailByDoctorAndDate(doctorId, date);
        applyRealtimeStock(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSchedule(Schedule schedule) {
        if (schedule.getDoctorId() == null || schedule.getScheduleDate() == null
                || schedule.getTimePeriod() == null) {
            throw new BusinessException("医生、出诊日期、时段均不能为空");
        }
        int max = schedule.getMaxNum() == null ? DEFAULT_MAX_NUM : schedule.getMaxNum();
        schedule.setScheduleId(null);
        schedule.setMaxNum(max);
        schedule.setRemainNum(max);
        schedule.setVersion(0);
        scheduleMapper.insert(schedule);
        refreshStock(schedule.getScheduleId(), max);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSchedule(Schedule schedule) {
        if (schedule.getScheduleId() == null) {
            throw new BusinessException("排班ID不能为空");
        }
        Schedule exists = scheduleMapper.selectById(schedule.getScheduleId());
        if (exists == null) {
            throw new BusinessException("排班不存在");
        }
        // 已挂出的号不能被覆盖掉
        int used = exists.getMaxNum() - exists.getRemainNum();
        int max = schedule.getMaxNum() == null ? exists.getMaxNum() : schedule.getMaxNum();
        if (max < used) {
            throw new BusinessException("该时段已挂出 " + used + " 个号，最大号源不能小于该值");
        }
        schedule.setRemainNum(max - used);
        scheduleMapper.updateById(schedule);
        refreshStock(schedule.getScheduleId(), max - used);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Integer scheduleId) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        Long registers = registerRecordMapper.selectCount(Wrappers.<RegisterRecord>lambdaQuery()
                .eq(RegisterRecord::getScheduleId, scheduleId));
        if (registers != null && registers > 0) {
            throw new BusinessException("该排班已有 " + registers + " 条挂号记录，无法删除");
        }
        scheduleMapper.deleteById(scheduleId);
        redisUtil.delete(Constants.REDIS_STOCK_PREFIX + scheduleId);
    }

    @Override
    public int warmUpStock() {
        List<Schedule> list = scheduleMapper.selectList(Wrappers.<Schedule>lambdaQuery()
                .ge(Schedule::getScheduleDate, LocalDate.now()));
        for (Schedule s : list) {
            refreshStock(s.getScheduleId(), s.getRemainNum());
        }
        return list.size();
    }

    /** 覆盖写入 Redis 号源 */
    private void refreshStock(Integer scheduleId, Integer remainNum) {
        redisUtil.set(Constants.REDIS_STOCK_PREFIX + scheduleId, String.valueOf(remainNum));
    }

    /** 余号以 Redis 为准，保证前端看到的是实时值 */
    private void applyRealtimeStock(List<Schedule> list) {
        for (Schedule s : list) {
            String value = redisUtil.get(Constants.REDIS_STOCK_PREFIX + s.getScheduleId());
            if (value != null) {
                try {
                    s.setRemainNum(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    log.warn("Redis 号源值非法：scheduleId={}, value={}", s.getScheduleId(), value);
                }
            }
        }
    }
}
