package com.bizreport.core.service.report;

import com.bizreport.core.dto.report.ReportCommand;
import com.bizreport.core.dto.report.ReportRequest;
import com.bizreport.core.dto.report.ReportResponse;
import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.rate.TaxRate;
import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.Reports;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.user.Users;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.repository.business.RateRepository;
import com.bizreport.core.repository.data.DataRepository;
import com.bizreport.core.repository.report.ReportRepository;
import com.bizreport.core.repository.business.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportService {
    private final UserRepository userRepo;
    private final DataRepository dataRepo;
    private final RateRepository rateRepo;
    private final ReportRepository reportRepo;

    private final Map<ReportType, TaxCalculator> calcs;

    public ReportService(UserRepository userRepo, DataRepository dataRepo, RateRepository rateRepo, ReportRepository reportRepo, List<TaxCalculator> calculatorList) {
        this.userRepo = userRepo;
        this.dataRepo = dataRepo;
        this.rateRepo = rateRepo;
        this.reportRepo = reportRepo;
        this.calcs = calculatorList.stream()
                .collect(Collectors.toMap(TaxCalculator::getType, Function.identity()));
    }

    @Transactional
    public ReportResponse batchAcc(ReportRequest request) {
        YearMonth startMon = request.getStartYearMonth();
        YearMonth endMon = request.getEndYearMonth();
        LocalDate deadline = Reports.getDeadline(request.getReportType(), startMon, endMon);

        Users user = getUser(request.getId());
        String period = request.getPeriod();

        if (LocalDate.now().isAfter(deadline)) {
            Reports report = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(user.getId(), request.getReportType(), PeriodType.ACCUMULATED, period)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_ALREADY_CLOSED));

            return ReportResponse.from(report);
        }

        ReportCommand command = new ReportCommand(user, request.getReportType(), PeriodType.ACCUMULATED, startMon, endMon, request.getPrepaidTax());
        ReportResponse response = generateReport(command);

        Reports report = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(user.getId(), request.getReportType(), PeriodType.ACCUMULATED, period)
                .orElse(Reports.create(user, request.getReportType(), PeriodType.ACCUMULATED, period));

        report.update(response.getTax(), response.getCalc());
        reportRepo.save(report);

        return response;
    }

    /**
     * Event/Time-Driven Batch
     */
    @Transactional(readOnly = true)
    public ReportResponse generateReport(ReportCommand command) {
        LocalDate startDt = command.startMon().atDay(1);
        LocalDate endDt = command.endMon().atEndOfMonth();

        List<Data> dataList = dataRepo.findAllByUserIdAndTransDtBetween(command.user().getId(), startDt, endDt);

        TaxRate rate = rateRepo.findFirstByIdIndCdOrderByIdYearDesc(command.user().getIndCd())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSING_INDUSTRY_CODE));

        TaxCalculator calculator = calcs.get(command.reportType());

        TaxCalculator.Result result = calculator.calc(
                command.user(),
                dataList,
                rate,
                command.getPrepaidTax()
        );

        result.calc().put("dataCount", dataList.size());

        if (command.periodType() == PeriodType.ACCUMULATED) {
            result.calc().put("deadline", Reports.getDeadline(command.reportType(), command.startMon(), command.endMon()).toString());
        }

        return ReportResponse.of(command.user().getId(), command.reportType(), command.periodType(), command.period(), result.tax(), result.calc());
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(ReportRequest request) {
        boolean isMonthly = (request.getEndMon() == null || request.getEndMon().isBlank());
        PeriodType periodType = isMonthly ? PeriodType.MONTHLY : PeriodType.ACCUMULATED;

        String period = request.getPeriod();
        YearMonth endMon = isMonthly ? request.getStartYearMonth() : request.getEndYearMonth();
        LocalDate deadline = Reports.getDeadline(request.getReportType(), endMon);

        Reports report = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(request.getId(), request.getReportType(), periodType, period)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        if (!LocalDate.now().isAfter(deadline)) {
            report.getCalc().put("isFinalized", false);
            report.getCalc().put("notice", "마감 전 리포트입니다. 추후 데이터 변동에 따라 세액이 달라질 수 있습니다.");

        } else {
            report.getCalc().put("isFinalized", true);
        }

        return ReportResponse.from(report);
    }

    // ==========================================
    // helper method
    // ==========================================

    private Users getUser(String id) {
        return userRepo.findById(id).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}