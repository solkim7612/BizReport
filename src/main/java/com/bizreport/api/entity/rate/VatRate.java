package com.bizreport.api.entity.rate;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Predicate;

@Getter
public enum VatRate {
    RT15(new BigDecimal("15"),
            category -> category.contains("소매업") || category.contains("재생용") || category.contains("음식점업")),
    RT20(new BigDecimal("20"),
            category -> category.contains("제조업") || category.contains("농업") || category.contains("소화물")),
    RT25(new BigDecimal("25"),
            category -> category.contains("숙박업")),
    RT30(new BigDecimal("30"),
            category -> category.contains("건설업") || (category.contains("운수") && !category.contains("소화물")) || category.contains("정보통신업")),
    RT40(new BigDecimal("40"),
            category -> category.contains("금융") || (category.contains("기술서비스업") && !category.contains("촬영")) || category.contains("임대 서비스") || category.contains("부동산")),
    DEFAULT(new BigDecimal("30"),
            category -> true);

    private final BigDecimal rate;
    private final Predicate<String> expression;

    VatRate(BigDecimal rate, Predicate<String> expression) {
        this.rate = rate;
        this.expression = expression;
    }

    public static BigDecimal findRateByCategory(String category) {
        return Arrays.stream(values())
                .filter(vatRate -> vatRate.expression.test(category))
                .findFirst()
                .orElse(DEFAULT)
                .getRate();
    }
}
