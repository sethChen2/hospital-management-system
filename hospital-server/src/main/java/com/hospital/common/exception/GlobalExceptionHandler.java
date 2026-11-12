package com.hospital.common.exception;

import com.hospital.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理：任何异常都返回 R 结构，避免前端拿到裸 500 页面
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return R.error(e.getStatus(), e.getMessage());
    }

    /** 唯一索引冲突：给出可读中文提示 */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突：{}", e.getMessage());
        return R.error("数据已存在，请勿重复提交");
    }

    /** 外键约束等完整性冲突 */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public R<Void> handleIntegrity(org.springframework.dao.DataIntegrityViolationException e) {
        log.warn("数据完整性冲突：{}", e.getMessage());
        return R.error("存在关联数据，操作被拒绝");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        return R.error(firstFieldMessage(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        return R.error(firstFieldMessage(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraint(ConstraintViolationException e) {
        String msg = "参数不合法";
        for (ConstraintViolation<?> v : e.getConstraintViolations()) {
            msg = v.getMessage();
            break;
        }
        return R.error(msg);
    }

    /** 缺少必填参数 / 参数类型不对：返回可读提示而不是「系统繁忙」 */
    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public R<Void> handleParam(Exception e) {
        String msg;
        if (e instanceof MissingServletRequestParameterException) {
            msg = "缺少必填参数：" + ((MissingServletRequestParameterException) e).getParameterName();
        } else {
            msg = "参数类型不正确：" + ((MethodArgumentTypeMismatchException) e).getName();
        }
        log.warn("参数异常：{}", msg);
        return R.error(msg);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.error("系统繁忙，请稍后重试");
    }

    private String firstFieldMessage(FieldError fieldError) {
        return fieldError == null ? "参数不合法" : fieldError.getDefaultMessage();
    }
}
