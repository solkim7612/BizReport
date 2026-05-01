package com.bizreport.core.dto.report;

import com.bizreport.core.entity.report.ReportType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReportRequest {
    private String id;                      // 사업자등록번호
    private ReportType reportType;          // 세금 종류 (VAT 또는 CIT)
    private String startMon;                // 리포트시작월
    private String endMon;                  // 리포트종료월
    private BigDecimal prepaidTax;          // 기납부세액

    public BigDecimal getPrepaidTax() {
        return prepaidTax != null ? prepaidTax : BigDecimal.ZERO;
    }
}