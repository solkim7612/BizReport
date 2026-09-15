package com.bizreport.api.controller.report;

import com.bizreport.core.dto.report.ReportRequest;
import com.bizreport.core.dto.report.ReportResponse;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService service;

    @GetMapping("/get/{id}")
    public ResponseEntity<List<ReportResponse>> get(
            @PathVariable String id,
            @ModelAttribute ReportRequest request) {

        if (request.getEndMon() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_PERIOD);
        }

        log.info("[REPORT] 리포트 통합 조회: ID={}, Type={}, EndMon={}", id, request.getReportType(), request.getEndMon());
        return ResponseEntity.ok(service.get(id, request));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<String> update(
            @PathVariable String id,
            @RequestBody ReportRequest request) {

        service.update(id, request);
        return ResponseEntity.ok("월별 및 누적 리포트 갱신 완료");
    }

    @PostMapping("/refresh/{id}")
    public ResponseEntity<Void> refresh(
            @PathVariable String id,
            @RequestBody ReportRequest request) {

        service.refresh(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/prepaid-tax/{id}")
    public ResponseEntity<Map<String, BigDecimal>> getPrepaidTax(
            @PathVariable String id,
            @ModelAttribute ReportRequest request) {

        return ResponseEntity.ok(Map.of("prepaidTax", service.getPrepaidTax(id, request)));
    }
}