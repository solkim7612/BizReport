package com.bizreport.api.domain.report;

import com.bizreport.core.dto.report.ReportRequest;
import com.bizreport.core.dto.report.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    @PostMapping("/batch/accumulated")
    public ResponseEntity<ReportResponse> generateAccumulated(@RequestBody ReportRequest request) {
        if (request.getStartMon() == null || request.getEndMon() == null) {
            throw new IllegalArgumentException("누적 리포트 생성 시 시작월과 종료월은 필수입니다.");
        }

        ReportResponse response = service.generateAccumulated(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable String id,
            @ModelAttribute ReportRequest request) {

        if (request.getStartMon() == null || request.getStartMon().isBlank()) {
            throw new IllegalArgumentException("리포트 조회 시 시작월(startMon)은 필수입니다.");
        }

        request.setId(id);

        ReportResponse response = service.getReport(request);
        return ResponseEntity.ok(response);
    }
}