package com.bizreport.core.dto.exception;

import com.bizreport.core.entity.exception.ErrorCode;

public record ErrorResponse(String code, String message) {
    public static ErrorResponse from(ErrorCode code) {
        return new ErrorResponse(code.name(), code.getMessage());
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
