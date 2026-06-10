package com.note.service.common.exception;

import com.note.service.common.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//全局异常处理
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理参数校验异常（@Valid 失败）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验失败: {}", message);
        return Result.error(ErrorCode.PARAM_VALIDATION_FAILED.getCode(), message);
    }

    // 处理业务异常（自定义）
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // 处理其他所有异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常: type={}, message={}", e.getClass().getName(), e.getMessage());
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}
