package com.bizreport.core.dto.data;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;

@Getter
@Setter
public class CardUploadRequest {
    private String id;
    private String cardNum;
    private String startMon;
    private String endMon;
    private MultipartFile file;

    public YearMonth getStartYearMonth() {
        return (startMon != null && !startMon.isBlank()) ? YearMonth.parse(startMon) : null;
    }

    public YearMonth getEndYearMonth() {
        return (endMon != null && !endMon.isBlank()) ? YearMonth.parse(endMon) : null;
    }
}
