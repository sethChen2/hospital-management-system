package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.Constants;
import com.hospital.common.R;
import com.hospital.entity.dto.BedApplyDTO;
import com.hospital.entity.dto.CheckBatchDTO;
import com.hospital.entity.dto.DiagnosisDTO;
import com.hospital.entity.dto.PrescriptionBatchDTO;
import com.hospital.entity.po.CheckItem;
import com.hospital.entity.po.Doctor;
import com.hospital.entity.po.Drug;
import com.hospital.entity.po.Prescription;
import com.hospital.entity.po.RegisterCheck;
import com.hospital.entity.po.RegisterRecord;
import com.hospital.entity.po.Schedule;
import com.hospital.entity.po.User;
import com.hospital.service.CheckItemService;
import com.hospital.service.DiagnosisService;
import com.hospital.service.DoctorService;
import com.hospital.service.DrugService;
import com.hospital.service.RegisterService;
import com.hospital.service.ScheduleService;
import com.hospital.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * 医生工作台控制层，统一前缀 /doctor。
 *
 * <p>15 个接口按用途分四组：</p>
 * <ol>
 *     <li>字典：药品、检查项目（开单下拉框用）；</li>
 *     <li>列表与详情：我的排班、我的挂号单、患者信息、单条挂号单详情；</li>
 *     <li>接诊操作：病历、处方（增删查）、检查单、住院、结束就诊；</li>
 *     <li>查询：患者历史就诊记录。</li>
 * </ol>
 *
 * <p>鉴权链路：JwtInterceptor 先按 URL 前缀 /doctor 校验角色必须是 DOCTOR，
 * 并把 token 里的 userId 放进 request attribute；Controller 再用
 * {@link #currentDoctorId(Integer)} 把 userId 换成 doctorId。
 * 也就是说前端无法伪造 doctorId，数据归属校验统一在 Service 层完成。</p>
 */
@RestController
@RequestMapping("/doctor")
public class DoctorController {

    /** 未指定日期时，默认展示未来 7 天排班 */
    private static final int DEFAULT_DAYS = 6;

    private final DoctorService doctorService;
    private final ScheduleService scheduleService;
    private final RegisterService registerService;
    private final DiagnosisService diagnosisService;
    private final UserService userService;
    private final DrugService drugService;
    private final CheckItemService checkItemService;

    public DoctorController(DoctorService doctorService, ScheduleService scheduleService,
                            RegisterService registerService, DiagnosisService diagnosisService,
                            UserService userService, DrugService drugService,
                            CheckItemService checkItemService) {
        this.doctorService = doctorService;
        this.scheduleService = scheduleService;
        this.registerService = registerService;
        this.diagnosisService = diagnosisService;
        this.userService = userService;
        this.drugService = drugService;
        this.checkItemService = checkItemService;
    }

    /** 开处方时选择药品用的字典 */
    @GetMapping("/findAllDrugList")
    public R<List<Drug>> findAllDrugList() {
        return R.ok(drugService.listAll());
    }

    /** 开检查单时选择项目的字典 */
    @GetMapping("/findAllCheckList")
    public R<List<CheckItem>> findAllCheckList() {
        return R.ok(checkItemService.listAll());
    }

    /** 我的排班：不传日期时默认返回今天起 7 天，传了日期就只查当天 */
    @GetMapping("/findMySchedules")
    public R<List<Schedule>> findMySchedules(
            @RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate) {
        Integer doctorId = currentDoctorId(userId);
        if (scheduleDate != null) {
            return R.ok(scheduleService.listByDoctorAndDate(doctorId, scheduleDate));
        }
        LocalDate today = LocalDate.now();
        return R.ok(scheduleService.listByDoctor(doctorId, today, today.plusDays(DEFAULT_DAYS)));
    }

    /**
     * 我的挂号单（就诊列表）。
     *
     * <p>强制带上当前医生 doctorId，所以只能查到自己名下的单子；
     * registerStatus / payStatus / keyword 都是可选筛选项。</p>
     */
    @GetMapping("/findMyOrders")
    public R<IPage<RegisterRecord>> findMyOrders(
            @RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String registerStatus,
            @RequestParam(required = false) String payStatus,
            @RequestParam(required = false) String keyword) {
        Integer doctorId = currentDoctorId(userId);
        return R.ok(registerService.pageOrders(page, size, null, doctorId,
                registerStatus, payStatus, keyword));
    }

    /**
     * 查看患者基本信息（工作台顶部的姓名、性别、手机号、身份证等）。
     *
     * <p>只返回角色为 PATIENT 的用户；User 实体上的密码字段是 @JsonIgnore，
     * 不会被序列化到响应里。</p>
     */
    @GetMapping("/findPatient")
    public R<User> findPatient(@RequestParam Integer patientUserId) {
        return R.ok(userService.getPatient(patientUserId));
    }

    /**
     * 接诊工作台：查看本条挂号单详情。
     *
     * <p>返回的内容除挂号单本身外，还带处方和检查明细（由 Service 组装）；
     * 这里再校验一次归属，防止直接改 URL 里的 registerId 偷看别人的单子。</p>
     */
    @GetMapping("/findOrder")
    public R<RegisterRecord> findOrder(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                                       @RequestParam Integer registerId) {
        Integer doctorId = currentDoctorId(userId);
        RegisterRecord record = registerService.getOrderDetail(registerId);
        if (record == null) {
            return R.error("挂号单不存在");
        }
        if (!doctorId.equals(record.getDoctorId())) {
            return R.error("无权查看该挂号单");
        }
        return R.ok(record);
    }

    /** 书写病历：保存诊断结论，首次保存会把状态从「待就诊」推进到「就诊中」 */
    @PostMapping("/submitDiagnosis")
    public R<Void> submitDiagnosis(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                                   @RequestBody @Valid DiagnosisDTO dto) {
        diagnosisService.submitDiagnosis(currentDoctorId(userId), dto.getRegisterId(),
                dto.getDiagnosis());
        return R.ok("病历已保存", null);
    }

    /** 批量开处方：请求体只传药品ID和数量，价格与小计由 Service 查库计算 */
    @PostMapping("/addPrescriptions")
    public R<Void> addPrescriptions(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                                    @RequestBody @Valid PrescriptionBatchDTO dto) {
        diagnosisService.addPrescriptions(currentDoctorId(userId), dto.getRegisterId(),
                dto.getPrescriptions());
        return R.ok("处方已提交", null);
    }

    /** 查看处方 */
    @GetMapping("/findPrescriptions")
    public R<List<Prescription>> findPrescriptions(
            @RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
            @RequestParam Integer registerId) {
        return R.ok(diagnosisService.findPrescriptions(currentDoctorId(userId), registerId));
    }

    /** 删除一行处方 */
    @PostMapping("/deletePrescription")
    public R<Void> deletePrescription(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                                      @RequestParam Integer prescriptionId) {
        diagnosisService.deletePrescription(currentDoctorId(userId), prescriptionId);
        return R.ok("已删除", null);
    }

    /** 批量开检查单 */
    @PostMapping("/addRegisterChecks")
    public R<Void> addRegisterChecks(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                                     @RequestBody @Valid CheckBatchDTO dto) {
        diagnosisService.addRegisterChecks(currentDoctorId(userId), dto.getRegisterId(),
                dto.getCheckIds());
        return R.ok("检查单已提交", null);
    }

    /** 查看检查单 */
    @GetMapping("/findRegisterChecks")
    public R<List<RegisterCheck>> findRegisterChecks(
            @RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
            @RequestParam Integer registerId) {
        return R.ok(diagnosisService.findRegisterChecks(currentDoctorId(userId), registerId));
    }

    /** 申请住院：后台自动挑一张空闲床并条件占用，无需前端指定床位 */
    @PostMapping("/applyBed")
    public R<Void> applyBed(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                            @RequestBody @Valid BedApplyDTO dto) {
        diagnosisService.applyBed(currentDoctorId(userId), dto.getRegisterId(),
                dto.getHospitalReason());
        return R.ok("住院申请已提交", null);
    }

    /** 结束就诊：状态置「已完成」并写结束时间，之后该单不可再修改 */
    @PostMapping("/finishRegister")
    public R<Void> finishRegister(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                                  @RequestParam Integer registerId) {
        diagnosisService.finishRegister(currentDoctorId(userId), registerId);
        return R.ok("就诊已结束", null);
    }

    /**
     * 患者历史就诊记录：只查当前医生本人接诊过的单子，不是全院病历。
     *
     * <p>前端从列表页点「既往」进入，patientUserId 由路由参数带过来。</p>
     */
    @GetMapping("/findHistoryRegisters")
    public R<List<RegisterRecord>> findHistoryRegisters(
            @RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
            @RequestParam Integer patientUserId) {
        return R.ok(diagnosisService.findHistoryRegisters(currentDoctorId(userId), patientUserId));
    }

    /**
     * token 里存的是登录用户 userId，而业务表关联的是 doctor_id，这里做一次转换。
     *
     * <p>刻意不接受前端传入的 doctorId，避免伪造身份操作其他医生的挂号单。
     * 医生档案不存在时 {@link DoctorService#getByUserId} 会抛业务异常。</p>
     */
    private Integer currentDoctorId(Integer userId) {
        Doctor doctor = doctorService.getByUserId(userId);
        return doctor.getDoctorId();
    }
}
