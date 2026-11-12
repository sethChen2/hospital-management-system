package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.Constants;
import com.hospital.common.exception.BusinessException;
import com.hospital.entity.po.Doctor;
import com.hospital.entity.po.RegisterCheck;
import com.hospital.entity.po.RegisterRecord;
import com.hospital.entity.po.Prescription;
import com.hospital.entity.po.Schedule;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.PrescriptionMapper;
import com.hospital.mapper.RegisterCheckMapper;
import com.hospital.mapper.RegisterRecordMapper;
import com.hospital.mapper.ScheduleMapper;
import com.hospital.service.RegisterService;
import com.hospital.utils.RedisUtil;
import com.hospital.utils.ReportPdfUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RegisterServiceImpl implements RegisterService {

    private final RegisterRecordMapper registerRecordMapper;
    private final ScheduleMapper scheduleMapper;
    private final DoctorMapper doctorMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final RegisterCheckMapper registerCheckMapper;
    private final RedisUtil redisUtil;

    private final DefaultRedisScript<Long> stockDecrScript;
    private final DefaultRedisScript<Long> stockIncrScript;
    private final DefaultRedisScript<Long> releaseLockScript;

    public RegisterServiceImpl(RegisterRecordMapper registerRecordMapper,
                               ScheduleMapper scheduleMapper,
                               DoctorMapper doctorMapper,
                               PrescriptionMapper prescriptionMapper,
                               RegisterCheckMapper registerCheckMapper,
                               RedisUtil redisUtil,
                               @Qualifier("stockDecrScript") DefaultRedisScript<Long> stockDecrScript,
                               @Qualifier("stockIncrScript") DefaultRedisScript<Long> stockIncrScript,
                               @Qualifier("releaseLockScript") DefaultRedisScript<Long> releaseLockScript) {
        this.registerRecordMapper = registerRecordMapper;
        this.scheduleMapper = scheduleMapper;
        this.doctorMapper = doctorMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.registerCheckMapper = registerCheckMapper;
        this.redisUtil = redisUtil;
        this.stockDecrScript = stockDecrScript;
        this.stockIncrScript = stockIncrScript;
        this.releaseLockScript = releaseLockScript;
    }

    /**
     * 挂号核心链路：
     * 分布式锁 → Lua 原子扣号 → 重复挂号校验 → 落库 + 乐观锁扣减 → 异常补偿 → finally 释放锁
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer addOrder(Integer patientUserId, Integer scheduleId, String symptom) {
        if (scheduleId == null) {
            throw new BusinessException("请选择要预约的排班");
        }
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        if (schedule.getScheduleDate().isBefore(LocalDate.now())) {
            throw new BusinessException("该排班日期已过期，无法挂号");
        }
        Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
        if (doctor == null || doctor.getIsOnJob() == null || doctor.getIsOnJob() != 1) {
            throw new BusinessException("该医生已停诊，无法挂号");
        }

        String lockKey = Constants.REDIS_LOCK_PREFIX + scheduleId;
        String lockValue = UUID.randomUUID().toString();
        String stockKey = Constants.REDIS_STOCK_PREFIX + scheduleId;
        boolean locked = false;

        try {
            locked = acquireLock(lockKey, lockValue);
            if (!locked) {
                throw new BusinessException("当前预约人数较多，请稍后重试");
            }

            // 1. Lua 原子「判断 + 扣减」
            Long remain = redisUtil.execute(stockDecrScript, stockKey);
            if (remain != null && remain == -2L) {
                // 2. key 不存在（如 Redis 重启），从 MySQL 回填后重试一次
                redisUtil.set(stockKey, String.valueOf(schedule.getRemainNum()));
                remain = redisUtil.execute(stockDecrScript, stockKey);
            }
            if (remain == null || remain == -1L) {
                throw new BusinessException("该时段号源已被约满，请选择其他时段");
            }

            Integer registerId;
            try {
                registerId = saveRegister(patientUserId, schedule, doctor, symptom);
            } catch (RuntimeException e) {
                // 3. 落库失败 → 把刚扣掉的号源加回去
                redisUtil.execute(stockIncrScript, stockKey);
                throw e;
            }
            log.info("挂号成功：patientUserId={}, scheduleId={}, registerId={}, 剩余号源={}",
                    patientUserId, scheduleId, registerId, remain);
            return registerId;
        } finally {
            if (locked) {
                // 4. 只释放自己持有的锁
                redisUtil.execute(releaseLockScript, lockKey, lockValue);
            }
        }
    }

    private boolean acquireLock(String lockKey, String lockValue) {
        for (int i = 0; i < Constants.LOCK_RETRY_TIMES; i++) {
            if (redisUtil.tryLock(lockKey, lockValue, Constants.LOCK_EXPIRE_MS)) {
                return true;
            }
            try {
                Thread.sleep(Constants.LOCK_RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /** 落库：挂号单 + 乐观锁扣减数据库号源 */
    private Integer saveRegister(Integer patientUserId, Schedule schedule, Doctor doctor, String symptom) {
        RegisterRecord exists = registerRecordMapper.selectOne(Wrappers.<RegisterRecord>lambdaQuery()
                .eq(RegisterRecord::getPatientUserId, patientUserId)
                .eq(RegisterRecord::getScheduleId, schedule.getScheduleId()));

        if (exists != null && !Constants.REG_CANCELED.equals(exists.getRegisterStatus())) {
            throw new BusinessException("您已预约过该时段，请勿重复挂号");
        }

        LocalDateTime appointTime = resolveAppointTime(schedule);
        Integer registerId;
        if (exists == null) {
            RegisterRecord record = new RegisterRecord();
            record.setPatientUserId(patientUserId);
            record.setDoctorId(schedule.getDoctorId());
            record.setScheduleId(schedule.getScheduleId());
            record.setAppointTime(appointTime);
            record.setRegisterStatus(Constants.REG_WAITING);
            record.setSymptom(symptom);
            record.setTotalCost(doctor.getRegisterFee());
            record.setPayStatus(Constants.PAY_UNPAID);
            record.setVersion(0);
            registerRecordMapper.insert(record);
            registerId = record.getRegisterId();
        } else {
            // 之前取消过：复用同一行，避免唯一索引 uk_reg_patient_schedule 冲突
            RegisterRecord reuse = new RegisterRecord();
            reuse.setRegisterId(exists.getRegisterId());
            reuse.setAppointTime(appointTime);
            reuse.setRegisterStatus(Constants.REG_WAITING);
            reuse.setSymptom(symptom);
            reuse.setTotalCost(doctor.getRegisterFee());
            reuse.setPayStatus(Constants.PAY_UNPAID);
            reuse.setVersion(exists.getVersion());
            if (registerRecordMapper.updateById(reuse) == 0) {
                throw new BusinessException("挂号失败，请重试");
            }
            registerId = exists.getRegisterId();
        }

        // 与 Redis 同源扣减数据库号源：取消/删除回补、Redis 预热都依赖该值，必须同步维护
        if (scheduleMapper.decreaseStock(schedule.getScheduleId()) == 0) {
            throw new BusinessException("该时段号源已被约满，请选择其他时段");
        }
        return registerId;
    }

    /** AM → 08:00，PM → 14:00 */
    private LocalDateTime resolveAppointTime(Schedule schedule) {
        LocalTime time = Constants.PERIOD_AM.equals(schedule.getTimePeriod())
                ? LocalTime.of(8, 0) : LocalTime.of(14, 0);
        return LocalDateTime.of(schedule.getScheduleDate(), time);
    }

    @Override
    public IPage<RegisterRecord> pageOrders(Integer page, Integer size, Integer patientUserId,
                                            Integer doctorId, String registerStatus,
                                            String payStatus, String keyword) {
        return registerRecordMapper.pageRecords(new Page<>(page, size), patientUserId, doctorId,
                registerStatus, payStatus, keyword);
    }

    @Override
    public RegisterRecord getOrderDetail(Integer registerId) {
        RegisterRecord record = registerRecordMapper.selectDetail(registerId);
        if (record == null) {
            throw new BusinessException("挂号单不存在");
        }
        record.setPrescriptionList(prescriptionMapper.selectByRegisterId(registerId));
        record.setCheckList(registerCheckMapper.selectByRegisterId(registerId));
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Integer registerId, Integer patientUserId) {
        RegisterRecord record = registerRecordMapper.selectById(registerId);
        if (record == null) {
            throw new BusinessException("挂号单不存在");
        }
        if (!record.getPatientUserId().equals(patientUserId)) {
            throw new BusinessException("无权操作他人的挂号单");
        }
        if (Constants.REG_FINISHED.equals(record.getRegisterStatus())) {
            throw new BusinessException("就诊已完成，无法取消");
        }
        if (Constants.REG_CANCELED.equals(record.getRegisterStatus())) {
            throw new BusinessException("该挂号单已取消");
        }
        if (Constants.PAY_PAID.equals(record.getPayStatus())) {
            throw new BusinessException("已缴费的挂号单无法取消");
        }

        Schedule schedule = scheduleMapper.selectById(record.getScheduleId());
        RegisterRecord update = new RegisterRecord();
        update.setRegisterId(registerId);
        update.setRegisterStatus(Constants.REG_CANCELED);
        update.setVersion(record.getVersion());
        if (registerRecordMapper.updateById(update) == 0) {
            throw new BusinessException("取消失败，请刷新后重试");
        }

        // 未过期的排班才把号源还回去（Redis 与 DB 同源回补）
        if (schedule != null && !schedule.getScheduleDate().isBefore(LocalDate.now())) {
            redisUtil.execute(stockIncrScript, Constants.REDIS_STOCK_PREFIX + schedule.getScheduleId());
            if (scheduleMapper.increaseStock(schedule.getScheduleId()) == 0) {
                log.warn("取消挂号回补数据库号源失败（已达最大号源）：scheduleId={}", schedule.getScheduleId());
            }
        }
        log.info("取消挂号：registerId={}", registerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Integer registerId, Integer patientUserId) {
        RegisterRecord record = registerRecordMapper.selectById(registerId);
        if (record == null) {
            throw new BusinessException("挂号单不存在");
        }
        if (!record.getPatientUserId().equals(patientUserId)) {
            throw new BusinessException("无权操作他人的挂号单");
        }
        if (Constants.REG_CANCELED.equals(record.getRegisterStatus())) {
            throw new BusinessException("已取消的挂号单无需缴费");
        }
        if (Constants.PAY_PAID.equals(record.getPayStatus())) {
            throw new BusinessException("该挂号单已缴费，请勿重复支付");
        }
        RegisterRecord update = new RegisterRecord();
        update.setRegisterId(registerId);
        update.setPayStatus(Constants.PAY_PAID);
        update.setVersion(record.getVersion());
        if (registerRecordMapper.updateById(update) == 0) {
            throw new BusinessException("缴费失败，请刷新后重试");
        }
        log.info("缴费成功：registerId={}, 金额={}", registerId, record.getTotalCost());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Integer registerId) {
        RegisterRecord record = registerRecordMapper.selectById(registerId);
        if (record == null) {
            throw new BusinessException("挂号单不存在");
        }
        // 未就诊且未过期的单子，删除时把号源还回去
        if (Constants.REG_WAITING.equals(record.getRegisterStatus())) {
            Schedule schedule = scheduleMapper.selectById(record.getScheduleId());
            if (schedule != null && !schedule.getScheduleDate().isBefore(LocalDate.now())) {
                redisUtil.execute(stockIncrScript, Constants.REDIS_STOCK_PREFIX + schedule.getScheduleId());
                scheduleMapper.increaseStock(schedule.getScheduleId());
            }
        }
        // 处方与检查单由外键 ON DELETE CASCADE 一并清理
        registerRecordMapper.deleteById(registerId);
        log.info("管理员删除挂号单：registerId={}", registerId);
    }

    @Override
    public List<Prescription> findPrescriptions(Integer patientUserId, Integer registerId) {
        requireMyOrder(patientUserId, registerId);
        return prescriptionMapper.selectByRegisterId(registerId);
    }

    @Override
    public List<RegisterCheck> findChecks(Integer patientUserId, Integer registerId) {
        requireMyOrder(patientUserId, registerId);
        return registerCheckMapper.selectByRegisterId(registerId);
    }

    @Override
    public byte[] buildReportPdf(Integer registerId) {
        return ReportPdfUtil.build(getOrderDetail(registerId));
    }

    @Override
    public void checkReportOwner(Integer patientUserId, Integer registerId) {
        requireMyOrder(patientUserId, registerId);
    }

    /** 校验挂号单归属该患者 */
    private void requireMyOrder(Integer patientUserId, Integer registerId) {
        RegisterRecord record = registerRecordMapper.selectById(registerId);
        if (record == null) {
            throw new BusinessException("挂号单不存在");
        }
        if (!record.getPatientUserId().equals(patientUserId)) {
            throw new BusinessException("无权查看他人的挂号单");
        }
    }
}
