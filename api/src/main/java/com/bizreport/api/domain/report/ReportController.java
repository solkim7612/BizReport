package com.bizreport.api.domain.report;

import com.bizreport.core.dto.report.ReportRequest;
import com.bizreport.core.dto.report.ReportResponse;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    @PostMapping("/batch/accumulated")
    public ResponseEntity<ReportResponse> batchAcc(@RequestBody ReportRequest request) {
        if (request.getStartMon() == null || request.getEndMon() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_PERIOD);
        }

        return ResponseEntity.ok(service.batchAcc(request));
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable String id,
            @ModelAttribute ReportRequest request) {

        if (request.getStartMon() == null || request.getStartMon().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REPORT_PERIOD);
        }

        request.setId(id);

        return ResponseEntity.ok(service.getReport(request));
    }
}