package com.bizreport.api.entity.rate;

import java.math.BigDecimal;
import java.util.Arrays;

//TODO: 수정
public enum CITTax {
    LEVEL_1(14_000_000, 0.06, 0),
    LEVEL_2(50_000_000, 0.15, 1_260_000),
    LEVEL_3(88_000_000, 0.24, 5_760_000),
    LEVEL_4(150_000_000, 0.35, 15_440_000),
    LEVEL_MAX(Long.MAX_VALUE, 0.38, 19_940_000);

    private final long maxProfit;
    private final double rate;
    private final long deduction;

    CITTax(long maxProfit, double rate, long deduction) {
        this.maxProfit = maxProfit;
        this.rate = rate;
        this.deduction = deduction;
    }

    public static BigDecimal calculateTax(BigDecimal profit) {
        CITTax bracket = Arrays.stream(values())
                .filter(b -> profit.compareTo(BigDecimal.valueOf(b.maxProfit)) <= 0)
                .findFirst()
                .orElse(LEVEL_MAX);

        BigDecimal tax = profit.multiply(BigDecimal.valueOf(bracket.rate))
                .subtract(BigDecimal.valueOf(bracket.deduction));

        return tax.max(BigDecimal.ZERO);
    }

    public static double getAppliedRate(BigDecimal profit) {
        return Arrays.stream(values())
                .filter(b -> profit.compareTo(BigDecimal.valueOf(b.maxProfit)) <= 0)
                .findFirst()
                .orElse(LEVEL_MAX).rate;
    }
}