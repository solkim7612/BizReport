package com.bizreport.core.repository.report;

import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.Report;
import com.bizreport.core.entity.report.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByUserIdAndReportTypeAndPeriodTypeAndPeriod(
            String userId, ReportType reportType, PeriodType periodType, String period);
}