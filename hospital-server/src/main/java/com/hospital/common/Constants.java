package com.hospital.common;

/**
 * 全局常量
 */
public final class Constants {

    private Constants() {
    }

    /* ---------------- 角色 ---------------- */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_DOCTOR = "DOCTOR";
    public static final String ROLE_PATIENT = "PATIENT";

    /* ---------------- 挂号状态 ---------------- */
    public static final String REG_WAITING = "待就诊";
    public static final String REG_IN_TREATMENT = "就诊中";
    public static final String REG_FINISHED = "已完成";
    public static final String REG_CANCELED = "已取消";

    /* ---------------- 缴费状态 ---------------- */
    public static final String PAY_UNPAID = "未缴费";
    public static final String PAY_PAID = "已缴费";

    /* ---------------- 病床状态 ---------------- */
    public static final String BED_FREE = "空闲";
    public static final String BED_USED = "占用";

    /* ---------------- 时段 ---------------- */
    public static final String PERIOD_AM = "AM";
    public static final String PERIOD_PM = "PM";

    /* ---------------- Token / 请求头 ---------------- */
    public static final String TOKEN_HEADER = "token";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_ACCOUNT = "account";

    /* ---------------- Redis Key ---------------- */
    /** 排班号源：hospital:schedule:stock:{scheduleId} */
    public static final String REDIS_STOCK_PREFIX = "hospital:schedule:stock:";
    /** 挂号分布式锁：hospital:lock:schedule:{scheduleId} */
    public static final String REDIS_LOCK_PREFIX = "hospital:lock:schedule:";
    /** 限流：hospital:limit:{api}:{account} */
    public static final String REDIS_LIMIT_PREFIX = "hospital:limit:";

    public static final long LOCK_EXPIRE_MS = 10_000L;
    public static final int LOCK_RETRY_TIMES = 60;
    public static final long LOCK_RETRY_INTERVAL_MS = 50L;
}
