package com.hospital.controller;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.Constants;
import com.hospital.common.R;
import com.hospital.common.exception.BusinessException;
import com.hospital.entity.dto.OrderDTO;
import com.hospital.entity.po.Bed;
import com.hospital.entity.po.Doctor;
import com.hospital.entity.po.Prescription;
import com.hospital.entity.po.RegisterCheck;
import com.hospital.entity.po.RegisterRecord;
import com.hospital.entity.po.Schedule;
import com.hospital.service.BedService;
import com.hospital.service.DoctorService;
import com.hospital.service.RegisterService;
import com.hospital.service.ScheduleService;
import com.hospital.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;

/**
 * 患者控制层
 */
@Slf4j
@RestController
@RequestMapping("/patient")
public class PatientController {

    /** 未指定日期时，默认展示未来 7 天排班 */
    private static final int DEFAULT_DAYS = 6;

    private final DoctorService doctorService;
    private final ScheduleService scheduleService;
    private final RegisterService registerService;
    private final BedService bedService;
    private final JwtUtil jwtUtil;

    public PatientController(DoctorService doctorService, ScheduleService scheduleService,
                             RegisterService registerService, BedService bedService,
                             JwtUtil jwtUtil) {
        this.doctorService = doctorService;
        this.scheduleService = scheduleService;
        this.registerService = registerService;
        this.bedService = bedService;
        this.jwtUtil = jwtUtil;
    }

    /** 第一步：科室列表 */
    @GetMapping("/findDepartments")
    public R<List<String>> findDepartments() {
        return R.ok(doctorService.listDepartments());
    }

    /** 第二步：按科室查医生 */
    @GetMapping("/findDoctorsByDepartment")
    public R<List<Doctor>> findDoctorsByDepartment(@RequestParam(required = false) String department,
                                                   @RequestParam(required = false) String keyword) {
        return R.ok(doctorService.listByDepartment(department, keyword));
    }

    /** 第三步：查该医生的排班（余号实时来自 Redis） */
    @GetMapping("/findSchedulesByDoctor")
    public R<List<Schedule>> findSchedulesByDoctor(
            @RequestParam Integer doctorId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate) {
        if (scheduleDate != null) {
            return R.ok(scheduleService.listByDoctorAndDate(doctorId, scheduleDate));
        }
        LocalDate today = LocalDate.now();
        return R.ok(scheduleService.listByDoctor(doctorId, today, today.plusDays(DEFAULT_DAYS)));
    }

    /* ==================== 预约挂号 ==================== */

    /** 提交挂号（限流 + Redis 号源扣减） */
    @PostMapping("/addOrder")
    public R<Integer> addOrder(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                               @RequestBody @Valid OrderDTO dto) {
        Integer registerId = registerService.addOrder(userId, dto.getScheduleId(), dto.getSymptom());
        return R.ok("挂号成功", registerId);
    }

    /** 我的挂号列表 */
    @GetMapping("/findMyOrders")
    public R<IPage<RegisterRecord>> findMyOrders(
            @RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String registerStatus,
            @RequestParam(required = false) String payStatus) {
        return R.ok(registerService.pageOrders(page, size, userId, null,
                registerStatus, payStatus, null));
    }

    /** 挂号单详情 */
    @GetMapping("/findOrderDetail")
    public R<RegisterRecord> findOrderDetail(
            @RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
            @RequestParam Integer registerId) {
        RegisterRecord record = registerService.getOrderDetail(registerId);
        if (!record.getPatientUserId().equals(userId)) {
            return R.error("无权查看他人的挂号单");
        }
        return R.ok(record);
    }

    /** 取消挂号 */
    @PostMapping("/cancelOrder")
    public R<Void> cancelOrder(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                               @RequestParam Integer registerId) {
        registerService.cancelOrder(registerId, userId);
        return R.ok("已取消挂号", null);
    }

    /** 缴费 */
    @PostMapping("/payOrder")
    public R<Void> payOrder(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
                            @RequestParam Integer registerId) {
        registerService.payOrder(registerId, userId);
        return R.ok("缴费成功", null);
    }

    /** 我的处方明细 */
    @GetMapping("/findPrescriptions")
    public R<List<Prescription>> findPrescriptions(
            @RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
            @RequestParam Integer registerId) {
        return R.ok(registerService.findPrescriptions(userId, registerId));
    }

    /** 我的检查单 */
    @GetMapping("/findChecks")
    public R<List<RegisterCheck>> findChecks(
            @RequestAttribute(Constants.CLAIM_USER_ID) Integer userId,
            @RequestParam Integer registerId) {
        return R.ok(registerService.findChecks(userId, registerId));
    }

    /** 我的住院信息 */
    @GetMapping("/findBed")
    public R<List<Bed>> findBed(@RequestAttribute(Constants.CLAIM_USER_ID) Integer userId) {
        return R.ok(bedService.listByPatient(userId));
    }

    /**
     * 就诊报告单（公开接口：浏览器下载无法带自定义 header，故在拦截器放行）。
     * 提供 token 查询参数时校验归属；不提供则按演示要求仍可下载。
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam Integer registerId,
                                      @RequestParam(required = false) String token) {
        if (token != null && !token.trim().isEmpty()) {
            Integer userId;
            try {
                userId = jwtUtil.getUserId(token);
            } catch (JWTVerificationException e) {
                throw new BusinessException("下载凭证已失效，请重新登录");
            }
            registerService.checkReportOwner(userId, registerId);
        }
        byte[] data = registerService.buildReportPdf(registerId);
        String filename;
        try {
            filename = URLEncoder.encode("就诊报告单_" + registerId + ".pdf", "UTF-8")
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            filename = "report_" + registerId + ".pdf";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename*=UTF-8''" + filename);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
