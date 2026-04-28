package com.bizreport.api.domian.report;

import com.bizreport.api.dto.report.ReportRequest;
import com.bizreport.api.entity.global.TaxType;
import com.bizreport.api.entity.data.Data;
import com.bizreport.api.entity.data.DataType;
import com.bizreport.api.entity.rate.RateId;
import com.bizreport.api.entity.rate.TaxRate;
import com.bizreport.api.entity.report.PeriodType;
import com.bizreport.api.entity.report.ReportType;
import com.bizreport.api.entity.user.User;
import com.bizreport.api.entity.report.Report;
import com.bizreport.api.exception.CustomException;
import com.bizreport.api.exception.ErrorCode;
import com.bizreport.api.repository.DataRepository;
import com.bizreport.api.repository.ReportRepository;
import com.bizreport.api.repository.TaxRateRepository;
import com.bizreport.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VATCalcService {
    private final UserRepository userRepo;
    private final DataRepository dataRepo;
    private final ReportRepository reportRepo;
    private final TaxRateRepository taxRateRepo;

    private record Result(BigDecimal amount, Map<String, Object> snapshot) {}

    @Transactional
    public Report generateAccumulated(ReportRequest request) {
        User user = getUser(request.getId());
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());
        String period = request.getStartDate() + "-" + request.getEndDate();

        LocalDate deadline = (endDate.getMonthValue() <= 6)
                ? LocalDate.of(endDate.getYear(), 7, 25)
                : LocalDate.of(endDate.getYear() + 1, 1, 25);

        if (LocalDate.now().isAfter(deadline)) {
            return reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(user.getId(), ReportType.VAT, PeriodType.ACCUMULATED, period)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_ALREADY_CLOSED));
        }

        return save(user, startDate, endDate, period, PeriodType.ACCUMULATED, deadline);
    }

    @Transactional
    public Report generateMonthly(String id, YearMonth targetMonth) {
        User user = getUser(id);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();
        return save(user, startDate, endDate, targetMonth.toString(), PeriodType.MONTHLY, null);
    }

    private Report save(User user, LocalDate startDate, LocalDate endDate, String period, PeriodType periodType, LocalDate deadline) {
        List<Data> dataList = dataRepo.findAllByUserIdAndTransDateBetween(user.getId(), startDate, endDate);

        Result result = (user.getTaxType() == TaxType.GENERAL)
                ? calcGeneral(dataList)
                : calcSimplified(user, dataList, String.valueOf(endDate.getYear()));

        result.snapshot().put("dataCount", dataList.size());
        if (deadline != null) result.snapshot().put("deadline", deadline.toString());

        Report report = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(user.getId(), ReportType.VAT, periodType, period)
                .orElse(Report.create(user, ReportType.VAT, periodType, period));

        report.update(result.amount(), result.snapshot());
        return reportRepo.save(report);
    }

    private Result calcGeneral(List<Data> dataList) {
        BigDecimal salesTax = sumBy(dataList, DataType.SALES, Data::getVatValue);
        BigDecimal purchaseTax = sumBy(dataList, DataType.PURCHASE, Data::getVatValue);

        Map<String, Object> calc = new HashMap<>();
        calc.put("taxType", "GENERAL_VAT");
        calc.put("salesTax", salesTax);
        calc.put("purchaseTax", purchaseTax);

        return new Result(salesTax.subtract(purchaseTax), calc);
    }

    private Result calcSimplified(User user, List<Data> dataList, String year) {
        if (user.getIndCd() == null) throw new CustomException(ErrorCode.MISSING_INDUSTRY_CODE);

        Map<String, Object> calc = new HashMap<>();
        TaxRate rate = taxRateRepo.findById(new RateId(user.getIndCd(), year))
                .orElseGet(() -> {
                    TaxRate fallback = taxRateRepo.findFirstByIndCdOrderByYearDesc(user.getIndCd())
                            .orElseThrow(() -> new CustomException(ErrorCode.MISSING_INDUSTRY_CODE));
                    calc.put("warning", "최근 " + fallback.getId().getYear() + "년 부가율 적용");
                    return fallback;
                });

        BigDecimal sales = sumBy(dataList, DataType.SALES, Data::getTotalPrice);
        BigDecimal salesTax = sales.multiply(rate.getVatRate()).multiply(BigDecimal.valueOf(0.1));

        BigDecimal purchase = dataList.stream()
                .filter(d -> d.getType() == DataType.PURCHASE)
                .filter(d -> d.getVatValue() != null && d.getVatValue().compareTo(BigDecimal.ZERO) > 0) // 부가세 공제 가능 항목만!
                .map(Data::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purchaseTax = purchase.multiply(rate.getVatRate()).multiply(BigDecimal.valueOf(0.1));

        calc.put("taxType", "SIMPLIFIED_VAT");
        calc.put("vatRate", rate.getVatRate());

        return new Result(salesTax.max(BigDecimal.ZERO), calc);
    }

    private User getUser(String id) {
        return userRepo.findById(id).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private BigDecimal sumBy(List<Data> dataList, DataType type, java.util.function.Function<Data, BigDecimal> mapper) {
        return dataList.stream().filter(d -> d.getType() == type).map(mapper).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}