package com.bizreport.core.entity.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // [User]
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // [Business]
    MISSING_INDUSTRY_CODE(HttpStatus.BAD_REQUEST, "정확한 세금 계산을 위해 업종코드(indCd) 등록이 필수입니다."),
    REPORT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "이미 신고 기한이 만료되어 마감된 리포트입니다."),

    // [Report]
    INVALID_REPORT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 리포트 유형입니다."),
    INVALID_REPORT_DEADLINE(HttpStatus.BAD_REQUEST, "마감기한이 맞지 않습니다."),
    INVALID_REPORT_PERIOD(HttpStatus.BAD_REQUEST, "리포트 조회기간이 맞지 않습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 기간의 리포트가 생성되지 않았습니다. 먼저 생성을 요청해주세요."),

    // [Batch]
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다. CSV 파일만 업로드 가능합니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 및 저장 중 오류가 발생했습니다."),
    BATCH_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배치 작업 대기열 등록에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // [External API]
    EXTERNAL_API_FAILED(HttpStatus.BAD_GATEWAY, "국세청 등 외부 API 연동 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    EXTERNAL_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 API 응답 시간이 초과되었습니다."),

    // [System & Database]
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    CONCURRENCY_CONFLICT(HttpStatus.CONFLICT, "다른 작업이 진행 중이거나 충돌이 발생했습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}