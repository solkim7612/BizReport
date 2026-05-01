package com.bizreport.api.domain.report;

import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.rate.TaxRate;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.user.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface TaxCalculator {
    ReportType getType();
    Result calc(User user, List<Data> dataList, TaxRate rate, BigDecimal prepaidTax);
    record Result(BigDecimal tax, Map<String, Object> calc) {}
}
