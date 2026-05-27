package com.bizreport.core.dto.data;

import com.bizreport.core.entity.data.DataMethod;
import com.bizreport.core.entity.data.DataType;
import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;

@Getter
@Setter
public class DataRequest {
    private String startMon;
    private String endMon;
    private DataType type;
    private DataMethod method;

    public YearMonth getStartYearMonth() {
        return YearMonth.parse(startMon);
    }

    public YearMonth getEndYearMonth() {
        return YearMonth.parse(endMon);
    }
}