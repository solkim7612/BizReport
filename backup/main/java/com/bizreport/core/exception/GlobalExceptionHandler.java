package com.bizreport.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(CustomException e) {
        log.warn("CustomException 발생: {}", e.getErrorCode().getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("code", e.getErrorCode().getCode());
        response.put("message", e.getErrorCode().getMessage());

        return new ResponseEntity<>(response, e.getErrorCode().getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Unhandled Exception 발생: ", e);

        Map<String, Object> response = new HashMap<>();
        response.put("code", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        response.put("message", ErrorCode.INTERNAL_SERVER_ERROR.getMessage());

        return new ResponseEntity<>(response, ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
    }
}