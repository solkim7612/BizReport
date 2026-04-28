package com.bizreport.api.dto.business;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StatusResponse {
    private String status_code; // "OK" 여부
    private int match_cnt;
    private List<Data> data;

    @Getter
    @Setter
    public static class Data {
        private String b_no;               // 사업자번호
        private String b_stt;              // 영업상태 (계속사업자, 휴업자, 폐업자)
        private String tax_type;           // 과세유형 (부가가치세 일반과세자 등)
        private String tax_type_change_dt; // 과세유형 전환일자 (YYYYMMDD)
    }
}
