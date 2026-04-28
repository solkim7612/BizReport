package com.bizreport.api.entity.rate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CITRate {
    RATE_6(new BigDecimal("14000000"), 6, new BigDecimal("0")),
    RATE_15(new BigDecimal("50000000"), 15, new BigDecimal("1260000")),
    RATE_24(new BigDecimal("88000000"), 24, new BigDecimal("5760000")),
    RATE_35(new BigDecimal("150000000"), 35, new BigDecimal("15440000")),
    RATE_38(new BigDecimal("300000000"), 38, new BigDecimal("19940000"));
    // 이후 세율은 성실신고의무자 이므로, 사용자 대상에서 제외

    private final BigDecimal taxbase;       // 과세표준
    private final int rate;                 // 세율 (%)
    private final BigDecimal deduction;     // 누진공제액

    public static BigDecimal calcTax(BigDecimal profit) {
        CITRate bracket = Arrays.stream(values())
                .filter(b -> profit.compareTo(b.taxbase) <= 0)
                .findFirst()
                .orElse(RATE_38);

        BigDecimal tax = profit.multiply(BigDecimal.valueOf(bracket.rate).divide(BigDecimal.valueOf(100)))
                .subtract(bracket.deduction);
        return tax.max(BigDecimal.ZERO);
    }

    public static double getAppliedRate(BigDecimal profit) {
        return Arrays.stream(values())
                .filter(b -> profit.compareTo(b.taxbase) <= 0)
                .findFirst()
                .orElse(RATE_38).rate;
    }
}