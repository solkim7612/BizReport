package com.bizreport.api.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class StatusResponse {
    private String status_code;
    private int match_cnt;
    private int request_cnt;
    private List<StatusData> data;

    @Getter
    @NoArgsConstructor
    public static class StatusData {
        private String b_no;
//        private String b_stt;
        private String b_stt_cd;            // status_code: 01 계속 02 휴업 03 폐업
//        private String tax_type;
        private String tax_type_cd;         // tax_type_code: 01 일반 02 간이
        private String tax_type_change_dt;  // 과세유형 전환일
    }
}
