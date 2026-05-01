package com.bizreport.core.entity.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum TaxType {
    GENERAL("01", "일반과세자"),
    SIMPLIFIED("02", "간이과세자");

    private final String code;
    private final String desc;

    public static TaxType ofCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equals(code))
                .findFirst()
                .orElse(GENERAL);
    }
}