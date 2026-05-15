package com.bizreport.core.dto.report;

import com.bizreport.core.entity.report.ReportType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.YearMonth;

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

    public YearMonth getStartYearMonth() {
        return (startMon != null && !startMon.isBlank()) ? YearMonth.parse(startMon) : null;
    }

    public YearMonth getEndYearMonth() {
        return (endMon != null && !endMon.isBlank()) ? YearMonth.parse(endMon) : null;
    }

    public String getPeriod() {
        if (endMon == null || endMon.isBlank() || startMon.equals(endMon)) {
            return startMon;
        }
        return startMon + "~" + endMon;
    }
}