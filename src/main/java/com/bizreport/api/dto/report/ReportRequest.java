package com.bizreport.api.dto.report;

import com.bizreport.api.entity.report.ReportType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {
    private String id;             // 사업자등록번호
    private ReportType reportType; // 세금 종류 (VAT 또는 CIT)
    private String startDate;      // 시작일 (예: "2024-01-01")
    private String endDate;        // 종료일 (예: "2024-06-30")
}