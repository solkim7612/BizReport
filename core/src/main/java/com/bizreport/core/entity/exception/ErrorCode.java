package com.bizreport.core.entity.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==========================================
    // [Business domain] 사업자 및 상태 관련
    // ==========================================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "등록되지 않은 사업자입니다."),
    INVALID_BUSINESS_NUMBER(HttpStatus.BAD_REQUEST, "유효하지 않은 사업자등록번호 형식입니다."),
    USER_ALREADY_CLOSED(HttpStatus.FORBIDDEN, "폐업된 사업자입니다. 신고 및 데이터 처리가 제한됩니다."),
    MISSING_INDUSTRY_CODE(HttpStatus.BAD_REQUEST, "정확한 세금 계산을 위해 업종코드(indCd) 등록이 필수입니다."),

    // ==========================================
    // [Data domain] 세무 데이터 관련
    // ==========================================
    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 세무 데이터를 찾을 수 없습니다."),
    DATA_ALREADY_CLOSED(HttpStatus.FORBIDDEN, "법정 신고 기한이 만료되어 수정/삭제가 불가능하게 잠긴 데이터입니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 및 처리 중 서버 오류가 발생했습니다."),
    OCR_EXTRACTION_FAILED(HttpStatus.BAD_REQUEST, "영수증 이미지에서 글자를 추출할 수 없습니다."),

    // ==========================================
    // [Report domain] 리포트(부가세, 종소세) 관련
    // ==========================================
    INVALID_REPORT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 리포트 유형(VAT, CIT)입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 기간의 리포트가 아직 생성되지 않았습니다. 조회를 위해 먼저 생성을 요청해주세요."),
    REPORT_ALREADY_CLOSED(HttpStatus.FORBIDDEN, "해당 과세 기간은 이미 마감되었습니다. 추가 신고나 데이터 등록이 불가합니다."),
    INVALID_REPORT_PERIOD(HttpStatus.BAD_REQUEST, "리포트 조회 기간(시작월, 종료월)이 올바르지 않습니다."),
    INVALID_REPORT_DEADLINE(HttpStatus.BAD_REQUEST, "마감기한이 맞지 않습니다."),

    // ==========================================
    // [Batch domain] 대기열
    // ==========================================
    BATCH_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배치 작업 대기열 등록에 실패했습니다. 잠시 후 다시 시도해주세요."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "업로드 가능한 최대 파일 크기를 초과했습니다."),

    // ==========================================
    // [External API] 외부 연동
    // ==========================================
    EXTERNAL_API_FAILED(HttpStatus.BAD_GATEWAY, "외부 API 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    EXTERNAL_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 API 응답 시간이 초과되었습니다."),

    // ==========================================
    // [System / Common] 공통 예외
    // ==========================================
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청 파라미터나 본문(Body)의 입력값이 잘못되었습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드 호출입니다."),
    CONCURRENCY_CONFLICT(HttpStatus.CONFLICT, "데이터 처리 중 충돌이 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에서 예상치 못한 치명적인 오류가 발생했습니다.")
    ;

    private final HttpStatus status;
    private final String message;
}