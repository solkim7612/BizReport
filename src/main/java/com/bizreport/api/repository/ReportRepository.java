package com.bizreport.api.repository;

import com.bizreport.api.entity.report.PeriodType;
import com.bizreport.api.entity.report.Report;
import com.bizreport.api.entity.report.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByUserIdAndReportTypeAndPeriodTypeAndPeriod(
            String userId, ReportType reportType, PeriodType periodType, String period);
}