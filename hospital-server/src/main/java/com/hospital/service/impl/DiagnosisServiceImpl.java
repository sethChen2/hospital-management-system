package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hospital.common.Constants;
import com.hospital.common.exception.BusinessException;
import com.hospital.entity.dto.PrescriptionDTO;
import com.hospital.entity.po.Bed;
import com.hospital.entity.po.CheckItem;
import com.hospital.entity.po.Doctor;
import com.hospital.entity.po.Drug;
import com.hospital.entity.po.Prescription;
import com.hospital.entity.po.RegisterCheck;
import com.hospital.entity.po.RegisterRecord;
import com.hospital.mapper.BedMapper;
import com.hospital.mapper.CheckItemMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.DrugMapper;
import com.hospital.mapper.PrescriptionMapper;
import com.hospital.mapper.RegisterCheckMapper;
import com.hospital.mapper.RegisterRecordMapper;
import com.hospital.service.DiagnosisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 医生接诊业务实现类
 *
 * <p>一次就诊的完整业务链都在这里：写病历 → 开处方 → 开检查单 → 申请住院 → 结束就诊，
 * 外加一个「患者历史就诊记录」查询。</p>
 *
 * <p>统一约定：</p>
 * <ol>
 *     <li>所有方法入口先用 {@link #requireOwned} / {@link #requireTreatable} 校验
 *         「这张挂号单属于当前医生」且「还没结束、没被取消」，防止越权和改历史数据；</li>
 *     <li>凡是会改变处方的操作，最后都调用 {@link #recalcTotalCost} 重算总费用，
 *         保证总价与明细始终一致；</li>
 *     <li>涉及挂号单的更新都带 version 走乐观锁，并发修改时后提交者会失败而不是覆盖别人。</li>
 * </ol>
 */
@Slf4j
@Service
public class DiagnosisServiceImpl implements DiagnosisService {

    /** 单次申请住院时的抢床重试次数 */
    private static final int BED_RETRY_TIMES = 5;

    private final RegisterRecordMapper registerRecordMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final RegisterCheckMapper registerCheckMapper;
    private final DoctorMapper doctorMapper;
    private final DrugMapper drugMapper;
    private final CheckItemMapper checkItemMapper;
    private final BedMapper bedMapper;

    public DiagnosisServiceImpl(RegisterRecordMapper registerRecordMapper,
                                PrescriptionMapper prescriptionMapper,
                                RegisterCheckMapper registerCheckMapper,
                                DoctorMapper doctorMapper,
                                DrugMapper drugMapper,
                                CheckItemMapper checkItemMapper,
                                BedMapper bedMapper) {
        this.registerRecordMapper = registerRecordMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.registerCheckMapper = registerCheckMapper;
        this.doctorMapper = doctorMapper;
        this.drugMapper = drugMapper;
        this.checkItemMapper = checkItemMapper;
        this.bedMapper = bedMapper;
    }

    /**
     * 书写 / 保存病历。
     *
     * <p>只更新诊断结论文本；如果当前还是「待就诊」，顺带把状态推进到「就诊中」，
     * 这就是"接诊"动作在后台留下的痕迹。重复保存不会改变已推进的状态。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDiagnosis(Integer doctorId, Integer registerId, String diagnosis) {
        // 校验：单子归当前医生，且未结束、未取消
        RegisterRecord record = requireTreatable(doctorId, registerId);

        // 只构造需要更新的字段，其余字段保持数据库原值（MyBatis-Plus 默认忽略 null 字段）
        RegisterRecord update = new RegisterRecord();
        update.setRegisterId(record.getRegisterId());
        update.setDiagnosis(diagnosis);
        // 首次保存病历 = 开始接诊，状态由「待就诊」推进到「就诊中」；已经是就诊中则不再改动
        if (Constants.REG_WAITING.equals(record.getRegisterStatus())) {
            update.setRegisterStatus(Constants.REG_IN_TREATMENT);
        }
        // 带上读取时的版本号，由 MyBatis-Plus 乐观锁插件比对，防止两个窗口同时保存互相覆盖
        update.setVersion(record.getVersion());
        if (registerRecordMapper.updateById(update) == 0) {
            // 影响行数为 0 说明版本已被别人改过，直接回滚并提示刷新
            throw new BusinessException("病历保存失败，请刷新后重试");
        }
        log.info("书写病历：registerId={}, doctorId={}", registerId, doctorId);
    }

    /**
     * 批量开处方。
     *
     * <p>前端只提交「药品ID + 数量」，单价、小计一律由服务端按数据库药价计算，
     * 防止有人改请求体把药价改低。任何一行不合法都会抛异常，整个事务回滚，
     * 不会出现"只开进去一半药"的情况。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPrescriptions(Integer doctorId, Integer registerId,
                                 List<PrescriptionDTO> prescriptions) {
        RegisterRecord record = requireTreatable(doctorId, registerId);

        // 先把这张单子已经开过的药装进集合，用于和本次提交做去重
        Set<Integer> seen = new HashSet<>();
        for (Prescription existing : prescriptionMapper.selectByRegisterId(registerId)) {
            seen.add(existing.getDrugId());
        }

        List<Prescription> toInsert = new ArrayList<>();
        for (PrescriptionDTO dto : prescriptions) {
            // HashSet.add 返回 false 说明这个药已经在库里或本次请求里出现过
            if (!seen.add(dto.getDrugId())) {
                Drug drug = drugMapper.selectById(dto.getDrugId());
                throw new BusinessException("药品「" + (drug == null ? dto.getDrugId() : drug.getDrugName())
                        + "」已在处方中，请直接修改数量");
            }
            Drug drug = drugMapper.selectById(dto.getDrugId());
            if (drug == null) {
                throw new BusinessException("药品不存在：" + dto.getDrugId());
            }
            if (drug.getPrice() == null) {
                throw new BusinessException("药品「" + drug.getDrugName() + "」未维护单价");
            }
            Prescription p = new Prescription();
            p.setRegisterId(registerId);
            p.setDrugId(drug.getDrugId());
            p.setDrugNum(dto.getDrugNum());
            // 小计 = 数据库单价 × 数量，作为下单时的价格快照落库，后续药品调价不影响这张处方
            p.setSubtotal(drug.getPrice().multiply(BigDecimal.valueOf(dto.getDrugNum())));
            toInsert.add(p);
        }
        // 全部行都校验通过后才真正写库，避免边校验边插入导致部分成功
        for (Prescription p : toInsert) {
            prescriptionMapper.insert(p);
        }
        // 明细变了，总费用必须跟着重算
        recalcTotalCost(record);
        log.info("批量开处方：registerId={}, 行数={}", registerId, toInsert.size());
    }

    /** 查看某张挂号单的处方明细：只读，但同样要确认单子属于当前医生 */
    @Override
    public List<Prescription> findPrescriptions(Integer doctorId, Integer registerId) {
        requireOwned(doctorId, registerId);
        return prescriptionMapper.selectByRegisterId(registerId);
    }

    /**
     * 删除一行处方明细。
     *
     * <p>处方明细表本身不存医生ID，所以要先按明细反查它属于哪张挂号单，
     * 再用那张单子做归属和状态校验，防止删别人的处方。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePrescription(Integer doctorId, Integer prescriptionId) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("处方明细不存在");
        }
        // 用明细上的 registerId 反查挂号单，校验归属 + 是否还能修改
        RegisterRecord record = requireTreatable(doctorId, prescription.getRegisterId());
        prescriptionMapper.deleteById(prescriptionId);
        // 删一行也要重算总费用
        recalcTotalCost(record);
        log.info("删除处方明细：prescriptionId={}", prescriptionId);
    }

    /**
     * 批量开检查单。
     *
     * <p>检查项目没有数量概念，开一次就是一项；同一项目不允许重复开，
     * 项目是否存在也要校验（防止前端传入不存在的检查ID）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRegisterChecks(Integer doctorId, Integer registerId, List<Integer> checkIds) {
        RegisterRecord record = requireTreatable(doctorId, registerId);

        // 这张单子已经开过的检查项目，用于去重
        Set<Integer> seen = new HashSet<>();
        for (RegisterCheck existing : registerCheckMapper.selectByRegisterId(registerId)) {
            seen.add(existing.getCheckId());
        }
        for (Integer checkId : checkIds) {
            // 已在库中或本次请求中重复出现
            if (!seen.add(checkId)) {
                CheckItem item = checkItemMapper.selectById(checkId);
                throw new BusinessException("检查项目「" + (item == null ? checkId : item.getCheckName())
                        + "」已开过，请勿重复");
            }
            // 检查项目必须存在于字典表，避免脏数据
            if (checkItemMapper.selectById(checkId) == null) {
                throw new BusinessException("检查项目不存在：" + checkId);
            }
            RegisterCheck rc = new RegisterCheck();
            rc.setRegisterId(registerId);
            rc.setCheckId(checkId);
            registerCheckMapper.insert(rc);
        }
        // 检查费用计入总费用
        recalcTotalCost(record);
        log.info("批量开检查单：registerId={}, 项目数={}", registerId, checkIds.size());
    }

    /** 查看某张挂号单已开的检查项目（只读，仍校验归属） */
    @Override
    public List<RegisterCheck> findRegisterChecks(Integer doctorId, Integer registerId) {
        requireOwned(doctorId, registerId);
        return registerCheckMapper.selectByRegisterId(registerId);
    }

    /**
     * 申请住院：不指定具体床位，由后台自动挑一张空闲床并占用。
     *
     * <p>这里用 {@link #requireOwned} 而不是 requireTreatable，因为住院可以在就诊结束后补办；
     * 但同一患者只能有一张在院床位，所以先查重。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyBed(Integer doctorId, Integer registerId, String hospitalReason) {
        RegisterRecord record = requireOwned(doctorId, registerId);

        // 防止同一患者重复住院：统计该患者在 bed 表中「使用中」的床位数量
        Long occupied = bedMapper.selectCount(Wrappers.<Bed>lambdaQuery()
                .eq(Bed::getPatientUserId, record.getPatientUserId())
                .eq(Bed::getBedStatus, Constants.BED_USED));
        if (occupied != null && occupied > 0) {
            throw new BusinessException("该患者已办理住院，无需重复申请");
        }

        // 抢床重试：先查一张空闲床，再尝试用条件更新占住它
        for (int i = 0; i < BED_RETRY_TIMES; i++) {
            Integer bedId = bedMapper.selectFreeBedId();
            if (bedId == null) {
                throw new BusinessException("暂无空闲床位，请稍后再试");
            }
            // occupyBed 的 SQL 带 where bed_status = '空闲'，返回 >0 才表示真的抢到
            if (bedMapper.occupyBed(bedId, record.getPatientUserId(), record.getDoctorId(),
                    LocalDateTime.now(), hospitalReason) > 0) {
                log.info("申请住院成功：registerId={}, bedId={}", registerId, bedId);
                return;
            }
            // 床位被并发抢走，换一张重试
        }
        throw new BusinessException("床位分配冲突，请稍后重试");
    }

    /**
     * 结束就诊：状态置为「已完成」并记录结束时间，之后这张单子不允许再修改。
     *
     * <p>不校验是否缴费——缴费是患者端动作，医生不该被它阻塞。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishRegister(Integer doctorId, Integer registerId) {
        RegisterRecord record = requireOwned(doctorId, registerId);
        // 终态校验：已结束、已取消的单子不能再次结束
        if (Constants.REG_FINISHED.equals(record.getRegisterStatus())) {
            throw new BusinessException("该挂号单已结束就诊");
        }
        if (Constants.REG_CANCELED.equals(record.getRegisterStatus())) {
            throw new BusinessException("该挂号单已取消，无法结束就诊");
        }
        RegisterRecord update = new RegisterRecord();
        update.setRegisterId(registerId);
        update.setRegisterStatus(Constants.REG_FINISHED);
        update.setEndTime(LocalDateTime.now());
        // 乐观锁，避免和"再次开处方/患者取消"等操作并发覆盖
        update.setVersion(record.getVersion());
        if (registerRecordMapper.updateById(update) == 0) {
            throw new BusinessException("结束就诊失败，请刷新后重试");
        }
        log.info("结束就诊：registerId={}, doctorId={}", registerId, doctorId);
    }

    /**
     * 患者历史就诊记录。
     *
     * <p>只返回「当前医生本人接诊过」的记录，不是全院病历，避免跨科室泄露患者隐私。</p>
     */
    @Override
    public List<RegisterRecord> findHistoryRegisters(Integer doctorId, Integer patientUserId) {
        return registerRecordMapper.selectHistoryByDoctor(doctorId, patientUserId);
    }

    /* ==================== 内部工具 ==================== */

    /**
     * 数据级权限校验：挂号单必须存在，且 doctorId 属于当前登录医生。
     *
     * <p>角色校验在 JwtInterceptor 完成（只放 DOCTOR 进来），这里解决"医生A看医生B的单子"。</p>
     *
     * @return 数据库中的原始记录，供调用方继续使用（含 version 字段）
     */
    private RegisterRecord requireOwned(Integer doctorId, Integer registerId) {
        RegisterRecord record = registerRecordMapper.selectById(registerId);
        if (record == null) {
            throw new BusinessException("挂号单不存在");
        }
        if (!record.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权操作其他医生的挂号单");
        }
        return record;
    }

    /**
     * 在 requireOwned 基础上再加状态校验：已完成、已取消的单子一律不允许再写病历/处方/检查。
     *
     * <p>这就是前端"已完成的单子点进去改不了"的真正原因。</p>
     */
    private RegisterRecord requireTreatable(Integer doctorId, Integer registerId) {
        RegisterRecord record = requireOwned(doctorId, registerId);
        if (Constants.REG_FINISHED.equals(record.getRegisterStatus())) {
            throw new BusinessException("就诊已完成，无法再修改");
        }
        if (Constants.REG_CANCELED.equals(record.getRegisterStatus())) {
            throw new BusinessException("挂号单已取消，无法操作");
        }
        return record;
    }

    /**
     * 重算挂号单总费用：挂号费 + 处方小计合计 + 检查费合计。
     *
     * <p>采用"每次全量重算"而不是"在原总额上加减"，处方或检查增删后不会出现重复累加；
     * 金额统一用 BigDecimal，避免浮点误差。两个 SUM 查询在 SQL 层用 IFNULL 兜底，
     * 这里再判一次 null 属于双保险。</p>
     */
    private void recalcTotalCost(RegisterRecord record) {
        // 挂号费取医生档案里的当前值（医生可能由管理员调价）
        Doctor doctor = doctorMapper.selectById(record.getDoctorId());
        BigDecimal fee = doctor == null || doctor.getRegisterFee() == null
                ? BigDecimal.ZERO : doctor.getRegisterFee();
        // 处方小计总和、检查费用总和
        BigDecimal drugSum = prescriptionMapper.sumSubtotalByRegisterId(record.getRegisterId());
        BigDecimal checkSum = registerCheckMapper.sumPriceByRegisterId(record.getRegisterId());
        BigDecimal total = fee
                .add(drugSum == null ? BigDecimal.ZERO : drugSum)
                .add(checkSum == null ? BigDecimal.ZERO : checkSum);

        RegisterRecord update = new RegisterRecord();
        update.setRegisterId(record.getRegisterId());
        update.setTotalCost(total);
        // 只更新总费用，同样走乐观锁，避免和并发写处方一起把总额算错
        update.setVersion(record.getVersion());
        if (registerRecordMapper.updateById(update) == 0) {
            throw new BusinessException("费用合计失败，请刷新后重试");
        }
    }
}
