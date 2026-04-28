package com.bizreport.api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // User Errors
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),

    // Tax Calculation Errors
    MISSING_INDUSTRY_CODE(HttpStatus.BAD_REQUEST, "T001", "정확한 세금 계산을 위해 업종코드(indCd) 등록이 필수입니다."),
    REPORT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "T002", "이미 신고 기한이 만료되어 마감된 리포트입니다."),

    // System Errors
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}