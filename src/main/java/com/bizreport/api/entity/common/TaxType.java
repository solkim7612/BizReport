package com.bizreport.api.entity.common;

public enum TaxType {
    GENERAL,
    SIMPLIFIED;

    public static TaxType parse(String code) {
        return "02".equals(code) ? SIMPLIFIED : GENERAL;
    }
}
