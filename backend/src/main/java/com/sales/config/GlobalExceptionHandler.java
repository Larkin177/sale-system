package com.sales.config;

import com.sales.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 参数校验异常 - 返回具体校验错误信息
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.error(400, message);
    }

    // 参数校验异常（@Validated on class level）
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("全局异常 [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        // 对于校验异常，返回具体错误信息（不暴露内部实现细节）
        if (e instanceof jakarta.validation.ValidationException
                || e instanceof MethodArgumentNotValidException
                || e instanceof ConstraintViolationException) {
            return ApiResponse.error(400, e.getMessage());
        }
        // 不向客户端暴露内部异常详情
        return ApiResponse.error(500, "服务器内部错误，请稍后重试");
    }
}
