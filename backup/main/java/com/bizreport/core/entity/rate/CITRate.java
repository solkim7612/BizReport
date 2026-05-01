package com.bizreport.core.entity.rate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CITRate {
    RT_6(new BigDecimal("14000000"), 6, new BigDecimal("0")),
    RT_15(new BigDecimal("50000000"), 15, new BigDecimal("1260000")),
    RT_24(new BigDecimal("88000000"), 24, new BigDecimal("5760000")),
    RT_35(new BigDecimal("150000000"), 35, new BigDecimal("15440000")),
    RT_38(new BigDecimal("300000000"), 38, new BigDecimal("19940000"));
    // 이후 세율은 성실신고의무자 이므로, 사용자 대상에서 제외

    private final BigDecimal max;               // 과세표준
    private final int rate;                     // 소득세율
    private final BigDecimal deduction;         // 누진공제액

    public static CITRate getMax(BigDecimal profit){
        return Arrays.stream(values())
                .filter(m->profit.compareTo(m.max)<=0)
                .findFirst()
                .orElse(RT_38);
    }

    public static BigDecimal calcTax(BigDecimal profit) {
        CITRate max=getMax(profit);

        BigDecimal tax = profit
                .multiply(BigDecimal.valueOf(max.rate).divide(BigDecimal.valueOf(100)))
                .subtract(max.deduction);

        return tax.max(BigDecimal.ZERO);
    }
}