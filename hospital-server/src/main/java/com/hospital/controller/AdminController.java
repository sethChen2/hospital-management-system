package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.R;
import com.hospital.entity.dto.DoctorDTO;
import com.hospital.entity.po.Bed;
import com.hospital.entity.po.CheckItem;
import com.hospital.entity.po.Doctor;
import com.hospital.entity.po.Drug;
import com.hospital.entity.po.RegisterRecord;
import com.hospital.entity.po.Schedule;
import com.hospital.entity.po.User;
import com.hospital.entity.vo.DoctorExcelVO;
import com.hospital.entity.vo.StatisticsVO;
import com.hospital.service.BedService;
import com.hospital.service.CheckItemService;
import com.hospital.service.DoctorService;
import com.hospital.service.DrugService;
import com.hospital.service.RegisterService;
import com.hospital.service.ScheduleService;
import com.hospital.service.StatisticsService;
import com.hospital.service.UserService;
import com.hospital.utils.ExcelUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理员控制层
 */
@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final DoctorService doctorService;
    private final UserService userService;
    private final DrugService drugService;
    private final CheckItemService checkItemService;
    private final BedService bedService;
    private final ScheduleService scheduleService;
    private final RegisterService registerService;
    private final StatisticsService statisticsService;

    public AdminController(DoctorService doctorService, UserService userService,
                           DrugService drugService, CheckItemService checkItemService,
                           BedService bedService, ScheduleService scheduleService,
                           RegisterService registerService, StatisticsService statisticsService) {
        this.doctorService = doctorService;
        this.userService = userService;
        this.drugService = drugService;
        this.checkItemService = checkItemService;
        this.bedService = bedService;
        this.scheduleService = scheduleService;
        this.registerService = registerService;
        this.statisticsService = statisticsService;
    }

    /** 运营统计 */
    @GetMapping("/getStatistics")
    public R<StatisticsVO> getStatistics() {
        return R.ok(statisticsService.getStatistics());
    }

    /* ==================== 医生管理 ==================== */

    @GetMapping("/findAllDoctors")
    public R<IPage<Doctor>> findAllDoctors(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String department,
                                           @RequestParam(required = false) Integer isOnJob) {
        return R.ok(doctorService.pageDoctors(page, size, keyword, department, isOnJob));
    }

    @GetMapping("/findDoctor")
    public R<Doctor> findDoctor(@RequestParam Integer doctorId) {
        return R.ok(doctorService.getDoctor(doctorId));
    }

    @PostMapping("/addDoctor")
    public R<Void> addDoctor(@RequestBody @Valid DoctorDTO dto) {
        doctorService.addDoctor(dto);
        return R.ok("新增医生成功", null);
    }

    @PostMapping("/modifyDoctor")
    public R<Void> modifyDoctor(@RequestBody @Valid DoctorDTO dto) {
        doctorService.updateDoctor(dto);
        return R.ok("修改医生成功", null);
    }

    @PostMapping("/deleteDoctor")
    public R<Void> deleteDoctor(@RequestParam Integer doctorId) {
        doctorService.deleteDoctor(doctorId);
        return R.ok("删除医生成功", null);
    }

    @GetMapping("/exportDoctor")
    public void exportDoctor(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String department,
                             @RequestParam(required = false) Integer isOnJob,
                             HttpServletResponse response) throws IOException {
        List<DoctorExcelVO> list = doctorService.listForExport(keyword, department, isOnJob);
        ExcelUtil.export(response, "医生信息表", "医生信息", DoctorExcelVO.class, list);
    }

    @PostMapping("/importDoctor")
    public R<String> importDoctor(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return R.error("请选择要导入的 Excel 文件");
        }
        int[] result = doctorService.importDoctors(file.getInputStream());
        return R.ok("导入完成：成功 " + result[0] + " 条，跳过 " + result[1] + " 条", null);
    }

    @GetMapping("/findDepartments")
    public R<List<String>> findDepartments() {
        return R.ok(doctorService.listDepartments());
    }

    /* ==================== 患者管理 ==================== */

    @GetMapping("/findAllPatients")
    public R<IPage<User>> findAllPatients(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer accountStatus) {
        return R.ok(userService.pagePatients(page, size, keyword, accountStatus));
    }

    @GetMapping("/findPatient")
    public R<User> findPatient(@RequestParam Integer userId) {
        return R.ok(userService.getPatient(userId));
    }

    @PostMapping("/deletePatient")
    public R<Void> deletePatient(@RequestParam Integer userId) {
        userService.deletePatient(userId);
        return R.ok("删除患者成功", null);
    }

    /* ==================== 药品管理 ==================== */

    @GetMapping("/findAllDrugs")
    public R<IPage<Drug>> findAllDrugs(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(drugService.pageDrugs(page, size, keyword));
    }

    @GetMapping("/findAllDrugList")
    public R<List<Drug>> findAllDrugList() {
        return R.ok(drugService.listAll());
    }

    @PostMapping("/addDrug")
    public R<Void> addDrug(@RequestBody Drug drug) {
        drugService.addDrug(drug);
        return R.ok("新增药品成功", null);
    }

    @PostMapping("/modifyDrug")
    public R<Void> modifyDrug(@RequestBody Drug drug) {
        drugService.updateDrug(drug);
        return R.ok("修改药品成功", null);
    }

    @PostMapping("/deleteDrug")
    public R<Void> deleteDrug(@RequestParam Integer drugId) {
        drugService.deleteDrug(drugId);
        return R.ok("删除药品成功", null);
    }

    /* ==================== 检查项目管理 ==================== */

    @GetMapping("/findAllChecks")
    public R<IPage<CheckItem>> findAllChecks(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size,
                                             @RequestParam(required = false) String keyword) {
        return R.ok(checkItemService.pageChecks(page, size, keyword));
    }

    @GetMapping("/findAllCheckList")
    public R<List<CheckItem>> findAllCheckList() {
        return R.ok(checkItemService.listAll());
    }

    @PostMapping("/addCheck")
    public R<Void> addCheck(@RequestBody CheckItem checkItem) {
        checkItemService.addCheck(checkItem);
        return R.ok("新增检查项目成功", null);
    }

    @PostMapping("/modifyCheck")
    public R<Void> modifyCheck(@RequestBody CheckItem checkItem) {
        checkItemService.updateCheck(checkItem);
        return R.ok("修改检查项目成功", null);
    }

    @PostMapping("/deleteCheck")
    public R<Void> deleteCheck(@RequestParam Integer checkId) {
        checkItemService.deleteCheck(checkId);
        return R.ok("删除检查项目成功", null);
    }

    /* ==================== 病床管理 ==================== */

    @GetMapping("/findAllBeds")
    public R<IPage<Bed>> findAllBeds(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size,
                                     @RequestParam(required = false) String bedStatus,
                                     @RequestParam(required = false) String keyword) {
        return R.ok(bedService.pageBeds(page, size, bedStatus, keyword));
    }

    @GetMapping("/findFreeBeds")
    public R<List<Bed>> findFreeBeds() {
        return R.ok(bedService.listFreeBeds());
    }

    @PostMapping("/addBed")
    public R<Void> addBed(@RequestBody Bed bed) {
        bedService.addBed(bed);
        return R.ok("新增病床成功", null);
    }

    @PostMapping("/modifyBed")
    public R<Void> modifyBed(@RequestBody Bed bed) {
        bedService.updateBed(bed);
        return R.ok("修改病床成功", null);
    }

    @PostMapping("/deleteBed")
    public R<Void> deleteBed(@RequestParam Integer bedId) {
        bedService.deleteBed(bedId);
        return R.ok("删除病床成功", null);
    }

    /* ==================== 排班管理 ==================== */

    @GetMapping("/findAllSchedules")
    public R<IPage<Schedule>> findAllSchedules(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer doctorId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(scheduleService.pageSchedules(page, size, doctorId, department, startDate, endDate));
    }

    @PostMapping("/addSchedule")
    public R<Void> addSchedule(@RequestBody Schedule schedule) {
        scheduleService.addSchedule(schedule);
        return R.ok("新增排班成功", null);
    }

    @PostMapping("/modifySchedule")
    public R<Void> modifySchedule(@RequestBody Schedule schedule) {
        scheduleService.updateSchedule(schedule);
        return R.ok("修改排班成功", null);
    }

    @PostMapping("/deleteSchedule")
    public R<Void> deleteSchedule(@RequestParam Integer scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return R.ok("删除排班成功", null);
    }

    /* ==================== 挂号管理 ==================== */

    @GetMapping("/findAllOrders")
    public R<IPage<RegisterRecord>> findAllOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String registerStatus,
            @RequestParam(required = false) String payStatus,
            @RequestParam(required = false) String keyword) {
        return R.ok(registerService.pageOrders(page, size, null, null,
                registerStatus, payStatus, keyword));
    }

    @GetMapping("/findOrder")
    public R<RegisterRecord> findOrder(@RequestParam Integer registerId) {
        return R.ok(registerService.getOrderDetail(registerId));
    }

    @PostMapping("/deleteOrder")
    public R<Void> deleteOrder(@RequestParam Integer registerId) {
        registerService.deleteOrder(registerId);
        return R.ok("删除挂号单成功", null);
    }
}
