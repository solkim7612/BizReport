package com.bizreport.api.domian.report;

import com.bizreport.api.dto.report.ReportRequest;
import com.bizreport.api.entity.report.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final VATCalcService vatCalc;
    private final CITCalcService citCalc;

    @PostMapping("/accumulated")
    public ResponseEntity<String> generateAccumulated(@RequestBody ReportRequest request) {
        if (request.getReportType() == ReportType.VAT) {
            vatCalc.generateAccumulated(request);
        } else {
            citCalc.generateAccumulated(request);
        }
        return ResponseEntity.ok(request.getReportType().name() + " 리포트 갱신 완료");
    }
}