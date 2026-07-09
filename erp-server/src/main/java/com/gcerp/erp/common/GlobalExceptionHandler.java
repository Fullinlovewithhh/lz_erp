package com.gcerp.erp.common;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 统一参数校验错误返回，便于前端直接展示。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidException(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ":" + e.getDefaultMessage())
                .orElse("参数校验失败");
        return ApiResponse.fail(msg);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.fail(ex.getMessage());
    }
}
