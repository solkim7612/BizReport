package com.bizreport.api.entity.user;

public enum BizStatus {
    CONTINUED,
    TEMP_CLOSED,
    CLOSED;

    public static BizStatus parse(String code){
        if("02".equals(code)) return TEMP_CLOSED;
        if("03".equals(code)) return CLOSED;
        return CONTINUED;
    }
}
