package com.hospital.common.exception;

/**
 * 业务异常：由 GlobalExceptionHandler 统一转成 R.error(msg)
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer status;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(Integer status, String message) {
        super(message);
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }
}
