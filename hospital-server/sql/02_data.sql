-- ============================================================
-- 医院管理系统 初始化数据脚本
-- 演示密码统一为 123456（BCrypt 哈希，已用 checkpw 校验通过）
-- 依赖：先执行 01_schema.sql
-- ============================================================
SET NAMES utf8mb4;
USE hospital;

-- 清空（按外键依赖倒序），便于反复重跑
DELETE FROM registercheck;
DELETE FROM prescription;
DELETE FROM registerrecord;
DELETE FROM schedule;
DELETE FROM bed;
DELETE FROM doctor;
DELETE FROM drug;
DELETE FROM checkitem;
DELETE FROM `user`;

-- ------------------------------------------------------------
-- 1. 用户：1 管理员 + 6 医生 + 3 患者
-- ------------------------------------------------------------
INSERT INTO `user`
(user_id, account, password, name, gender, phone, email, idcard, birth, role, account_status)
VALUES
(1,  'admin',     '$2a$10$YZjo0Jb.fRxux6uWv34cOOymEHqT9sNNK.DHsOOCP.0ny90A4tVKq', '张明',   '男', '13800000001', 'admin@hospital.com',    '110101198001010011', '1980-01-01', 'ADMIN',   1),
(2,  'doctor01',  '$2a$10$YZjo0Jb.fRxux6uWv34cOOymEHqT9sNNK.DHsOOCP.0ny90A4tVKq', '李文华', '男', '13800000002', 'doctor01@hospital.com', '110101197505120022', '1975-05-12', 'DOCTOR',  1),
(3,  'doctor02',  '$2a$10$YZjo0Jb.fRxux6uWv34cOOymEHqT9sNNK.DHsOOCP.0ny90A4tVKq', '王建国', '男', '13800000003', 'doctor02@hospital.com', '110101197808200033', '1978-08-20', 'DOCTOR',  1),
(4,  'doctor03',  '$2a$10$YZjo0Jb.fRxux6uWv34cOOymEHqT9sNNK.DHsOOCP.0ny90A4tVKq', '赵晓丽', '女', '13800000004', 'doctor03@hospital.com', '110101198203150044', '1982-03-15', 'DOCTOR',  1),
(5,  'doctor04',  '$2a$10$YZjo0Jb.fRxux6uWv34cOOymEHqT9sNNK.DHsOOCP.0ny90A4tVKq', '陈志强', '男', '13800000005', 'doctor04@hospital.com', '110101197911080055', '1979-11-08', 'DOCTOR',  1),
(6,  'doctor05',  '$2a$10$YZjo0Jb.fRxux6uWv34cOOymEHqT9sNNK.DHsOOCP.0ny90A4tVKq', '刘雅琴', '女', '13800000006', 'doctor05@hospital.com', '110101198506250066', '1985-06-25', 'DOCTOR',  1),
(7,  'doctor06',  '$2a$10$YZjo0Jb.fRxux6uWv34cOOymEHqT9sNNK.DHsOOCP.0ny90A4tVKq', '孙国平', '男', '13800000007', 'doctor06@hospital.com', '110101197401300077', '1974-01-30', 'DOCTOR',  1),
(8,  'patient01', '$2a$10$YZjo0Jb.fRxux6uWv34cOOymEHqT9sNNK.DHsOOCP.0ny90A4tVKq', '张小飞', '男', '13900000001', 'patient01@163.com',     '110101200005050088', '2000-05-05', 'PATIENT', 1),
(9,  'patient02', '$2a$10$YZjo0Jb.fRxux6uWv34cOOymEHqT9sNNK.DHsOOCP.0ny90A4tVKq', '李小红', '女', '13900000002', 'patient02@163.com',     '110101199808120099', '1998-08-12', 'PATIENT', 1),
(10, 'patient03', '$2a$10$YZjo0Jb.fRxux6uWv34cOOymEHqT9sNNK.DHsOOCP.0ny90A4tVKq', '王大力', '男', '13900000003', 'patient03@163.com',     '110101199203280100', '1992-03-28', 'PATIENT', 1);

-- ------------------------------------------------------------
-- 2. 医生档案（doctor_id 1..6 对应 user_id 2..7）
-- ------------------------------------------------------------
INSERT INTO doctor (doctor_id, user_id, position, department, register_fee, is_on_job)
VALUES
(1, 2, '主任医师', '内科',   30.00, 1),
(2, 3, '副主任医师', '外科', 35.00, 1),
(3, 4, '主治医师', '儿科',   25.00, 1),
(4, 5, '主任医师', '骨科',   40.00, 1),
(5, 6, '主治医师', '皮肤科', 20.00, 1),
(6, 7, '副主任医师', '眼科', 30.00, 1);

-- ------------------------------------------------------------
-- 3. 药品 10 种
-- ------------------------------------------------------------
INSERT INTO drug (drug_name, price, unit) VALUES
('阿莫西林胶囊',   18.50, '盒'),
('布洛芬缓释胶囊', 25.00, '盒'),
('头孢克肟片',     32.80, '盒'),
('感冒灵颗粒',     15.60, '盒'),
('奥美拉唑肠溶胶囊',28.00, '盒'),
('蒙脱石散',       12.00, '盒'),
('氯雷他定片',     21.50, '盒'),
('维生素C片',       9.80, '瓶'),
('复方甘草片',     11.20, '瓶'),
('阿奇霉素分散片', 36.00, '盒');

-- ------------------------------------------------------------
-- 4. 检查项目 8 项
-- ------------------------------------------------------------
INSERT INTO checkitem (check_name, price) VALUES
('血常规',       25.00),
('尿常规',       20.00),
('肝功能检查',   80.00),
('肾功能检查',   75.00),
('心电图',       40.00),
('胸部X光',      60.00),
('腹部B超',      90.00),
('CT检查',      280.00);

-- ------------------------------------------------------------
-- 5. 排班：6 名医生 × 近 7 天（含今天）× AM/PM = 84 条
--    max_num = remain_num = 40
-- ------------------------------------------------------------
INSERT INTO schedule (doctor_id, schedule_date, time_period, max_num, remain_num, version)
SELECT d.doctor_id,
       DATE_ADD(CURDATE(), INTERVAL n.d DAY),
       t.period,
       40, 40, 0
FROM doctor d
CROSS JOIN (
    SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL
    SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) n
CROSS JOIN (
    SELECT 'AM' AS period UNION ALL SELECT 'PM'
) t;

-- ------------------------------------------------------------
-- 6. 病床 10 张（全部空闲）
-- ------------------------------------------------------------
INSERT INTO bed (patient_user_id, doctor_id, bed_status, hospital_time, hospital_reason, version) VALUES
(NULL, NULL, '空闲', NULL, NULL, 0),
(NULL, NULL, '空闲', NULL, NULL, 0),
(NULL, NULL, '空闲', NULL, NULL, 0),
(NULL, NULL, '空闲', NULL, NULL, 0),
(NULL, NULL, '空闲', NULL, NULL, 0),
(NULL, NULL, '空闲', NULL, NULL, 0),
(NULL, NULL, '空闲', NULL, NULL, 0),
(NULL, NULL, '空闲', NULL, NULL, 0),
(NULL, NULL, '空闲', NULL, NULL, 0),
(NULL, NULL, '空闲', NULL, NULL, 0);
