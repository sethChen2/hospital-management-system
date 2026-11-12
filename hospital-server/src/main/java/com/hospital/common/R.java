package com.hospital.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应包装
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：200 成功，500 业务/系统异常 */
    private Integer status;
    /** 提示信息 */
    private String msg;
    /** 业务数据 */
    private T data;

    public static <T> R<T> ok() {
        return build(200, "操作成功", null);
    }

    public static <T> R<T> ok(T data) {
        return build(200, "操作成功", data);
    }

    public static <T> R<T> ok(String msg, T data) {
        return build(200, msg, data);
    }

    public static <T> R<T> error(String msg) {
        return build(500, msg, null);
    }

    public static <T> R<T> error(Integer status, String msg) {
        return build(status, msg, null);
    }

    private static <T> R<T> build(Integer status, String msg, T data) {
        R<T> r = new R<>();
        r.setStatus(status);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
