package com.bizreport.core.repository.report;

import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.Reports;
import com.bizreport.core.entity.report.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Reports, Long> {

    Optional<Reports> findByUserIdAndReportTypeAndPeriodTypeAndPeriod(
            String userId, ReportType reportType, PeriodType periodType, String period);
}