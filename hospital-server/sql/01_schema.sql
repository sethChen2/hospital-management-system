-- ============================================================
-- 医院管理系统 建库建表脚本
-- 依据：er图.png（9 张表，表名统一小写）
-- 字符集 utf8mb4，引擎 InnoDB，金额统一 DECIMAL(10,2)
-- ============================================================
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS hospital DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE hospital;

-- 按外键依赖倒序删除
DROP TABLE IF EXISTS registercheck;
DROP TABLE IF EXISTS prescription;
DROP TABLE IF EXISTS registerrecord;
DROP TABLE IF EXISTS schedule;
DROP TABLE IF EXISTS bed;
DROP TABLE IF EXISTS doctor;
DROP TABLE IF EXISTS drug;
DROP TABLE IF EXISTS checkitem;
DROP TABLE IF EXISTS `user`;

-- ------------------------------------------------------------
-- 1. 用户表（管理员 / 医生 / 患者的公共账号信息）
-- ------------------------------------------------------------
CREATE TABLE `user` (
    user_id        INT           NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    account        VARCHAR(50)   NOT NULL                COMMENT '登录账号',
    password       VARCHAR(100)  NOT NULL                COMMENT '密码（BCrypt）',
    name           VARCHAR(50)   NOT NULL                COMMENT '姓名',
    gender         VARCHAR(4)    NOT NULL DEFAULT '男'    COMMENT '性别',
    phone          VARCHAR(20)   DEFAULT NULL            COMMENT '手机号',
    email          VARCHAR(50)   DEFAULT NULL            COMMENT '邮箱',
    idcard         VARCHAR(20)   DEFAULT NULL            COMMENT '身份证号',
    birth          DATE          DEFAULT NULL            COMMENT '出生日期',
    role           VARCHAR(20)   NOT NULL                COMMENT '角色：ADMIN/DOCTOR/PATIENT',
    account_status TINYINT       NOT NULL DEFAULT 1      COMMENT '账号状态：1正常 0禁用',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_account (account)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 医生表（user 的 1:1 扩展，主键 doctor_id）
-- ------------------------------------------------------------
CREATE TABLE doctor (
    doctor_id    INT           NOT NULL AUTO_INCREMENT COMMENT '医生ID',
    user_id      INT           NOT NULL                COMMENT '关联用户ID',
    position     VARCHAR(50)   DEFAULT NULL            COMMENT '职称',
    department   VARCHAR(50)   DEFAULT NULL            COMMENT '科室（不建字典表，存字符串）',
    register_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '挂号费',
    is_on_job    TINYINT       NOT NULL DEFAULT 1      COMMENT '在职状态：1在职 0离职',
    PRIMARY KEY (doctor_id),
    UNIQUE KEY uk_doctor_user (user_id),
    KEY idx_doctor_department (department),
    CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES `user` (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '医生表';

-- ------------------------------------------------------------
-- 3. 药品表
-- ------------------------------------------------------------
CREATE TABLE drug (
    drug_id   INT           NOT NULL AUTO_INCREMENT COMMENT '药品ID',
    drug_name VARCHAR(50)   NOT NULL                COMMENT '药品名称',
    price     DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '单价',
    unit      VARCHAR(10)   DEFAULT NULL            COMMENT '单位',
    PRIMARY KEY (drug_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '药品表';

-- ------------------------------------------------------------
-- 4. 检查项目表
-- ------------------------------------------------------------
CREATE TABLE checkitem (
    check_id   INT           NOT NULL AUTO_INCREMENT COMMENT '检查项目ID',
    check_name VARCHAR(50)   NOT NULL                COMMENT '检查项目名称',
    price      DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '检查费用',
    PRIMARY KEY (check_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '检查项目表';

-- ------------------------------------------------------------
-- 5. 排班表（doctor 1:n schedule；号源存 remain_num，version 乐观锁）
-- ------------------------------------------------------------
CREATE TABLE schedule (
    schedule_id   INT         NOT NULL AUTO_INCREMENT COMMENT '排班ID',
    doctor_id     INT         NOT NULL                COMMENT '医生ID',
    schedule_date DATE        NOT NULL                COMMENT '出诊日期',
    time_period   VARCHAR(10) NOT NULL                COMMENT '时段：AM/PM',
    max_num       INT         NOT NULL DEFAULT 40     COMMENT '最大号源',
    remain_num    INT         NOT NULL DEFAULT 40     COMMENT '剩余号源',
    version       INT         NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (schedule_id),
    UNIQUE KEY uk_sched_doctor_date_period (doctor_id, schedule_date, time_period),
    KEY idx_sched_date (schedule_date),
    CONSTRAINT fk_sched_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (doctor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '排班表';

-- ------------------------------------------------------------
-- 6. 挂号记录表（患者预约产生；version 乐观锁）
-- ------------------------------------------------------------
CREATE TABLE registerrecord (
    register_id     INT           NOT NULL AUTO_INCREMENT COMMENT '挂号ID',
    patient_user_id INT           NOT NULL                COMMENT '患者用户ID',
    doctor_id       INT           NOT NULL                COMMENT '医生ID',
    schedule_id     INT           NOT NULL                COMMENT '排班ID',
    appoint_time    DATETIME      DEFAULT NULL            COMMENT '预约就诊时间',
    register_status VARCHAR(20)   NOT NULL DEFAULT '待就诊' COMMENT '状态：待就诊/就诊中/已完成/已取消',
    diagnosis       VARCHAR(500)  DEFAULT NULL            COMMENT '诊断结果',
    symptom         VARCHAR(500)  DEFAULT NULL            COMMENT '症状描述',
    end_time        DATETIME      DEFAULT NULL            COMMENT '就诊结束时间',
    total_cost      DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '总费用（挂号费+药费+检查费）',
    pay_status      VARCHAR(10)   NOT NULL DEFAULT '未缴费' COMMENT '缴费状态：未缴费/已缴费',
    version         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (register_id),
    UNIQUE KEY uk_reg_patient_schedule (patient_user_id, schedule_id),
    KEY idx_reg_doctor (doctor_id),
    KEY idx_reg_status (register_status),
    CONSTRAINT fk_reg_patient FOREIGN KEY (patient_user_id) REFERENCES `user` (user_id),
    CONSTRAINT fk_reg_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (doctor_id),
    CONSTRAINT fk_reg_schedule FOREIGN KEY (schedule_id) REFERENCES schedule (schedule_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '挂号记录表';

-- ------------------------------------------------------------
-- 7. 处方明细表（registerrecord 1:n prescription，drug 1:n prescription）
-- ------------------------------------------------------------
CREATE TABLE prescription (
    prescription_id INT           NOT NULL AUTO_INCREMENT COMMENT '处方明细ID',
    register_id     INT           NOT NULL                COMMENT '挂号ID',
    drug_id         INT           NOT NULL                COMMENT '药品ID',
    drug_num        INT           NOT NULL DEFAULT 1      COMMENT '数量',
    subtotal        DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '小计',
    PRIMARY KEY (prescription_id),
    UNIQUE KEY uk_presc_reg_drug (register_id, drug_id),
    CONSTRAINT fk_presc_reg FOREIGN KEY (register_id) REFERENCES registerrecord (register_id) ON DELETE CASCADE,
    CONSTRAINT fk_presc_drug FOREIGN KEY (drug_id) REFERENCES drug (drug_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '处方明细表';

-- ------------------------------------------------------------
-- 8. 检查单表（registerrecord 1:n registercheck，checkitem 1:n registercheck）
-- ------------------------------------------------------------
CREATE TABLE registercheck (
    rc_id       INT NOT NULL AUTO_INCREMENT COMMENT '检查单ID',
    register_id INT NOT NULL                COMMENT '挂号ID',
    check_id    INT NOT NULL                COMMENT '检查项目ID',
    PRIMARY KEY (rc_id),
    UNIQUE KEY uk_rc_reg_check (register_id, check_id),
    CONSTRAINT fk_rc_reg FOREIGN KEY (register_id) REFERENCES registerrecord (register_id) ON DELETE CASCADE,
    CONSTRAINT fk_rc_check FOREIGN KEY (check_id) REFERENCES checkitem (check_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '检查单表';

-- ------------------------------------------------------------
-- 9. 病床表（医生"管理"、患者"占用"；version 乐观锁）
-- ------------------------------------------------------------
CREATE TABLE bed (
    bed_id          INT          NOT NULL AUTO_INCREMENT COMMENT '病床ID',
    patient_user_id INT          DEFAULT NULL            COMMENT '占用患者用户ID',
    doctor_id       INT          DEFAULT NULL            COMMENT '主管医生ID',
    bed_status      VARCHAR(10)  NOT NULL DEFAULT '空闲'  COMMENT '状态：空闲/占用',
    hospital_time   DATETIME     DEFAULT NULL            COMMENT '住院时间',
    hospital_reason VARCHAR(255) DEFAULT NULL            COMMENT '住院原因',
    version         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (bed_id),
    KEY idx_bed_patient (patient_user_id),
    CONSTRAINT fk_bed_patient FOREIGN KEY (patient_user_id) REFERENCES `user` (user_id),
    CONSTRAINT fk_bed_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (doctor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '病床表';
