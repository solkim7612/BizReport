package com.bizreport.core.entity.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum Status {
    CONTINUED("01", "계속사업자"),
    TEMP_CLOSED("02", "휴업자"),
    CLOSED("03", "폐업자");

    private final String code;
    private final String desc;

    public static Status ofCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElse(CONTINUED);
    }
}
