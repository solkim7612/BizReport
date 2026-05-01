package com.bizreport.core.dto.business;

import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.TaxType;
import com.bizreport.core.entity.user.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
public class StatusResponse {
    private String status_code;
    private List<Data> data;

    @Getter
    @Setter
    public static class Data {
        private String b_no;                        // 사업자번호
        private String b_stt_cd;                    // 영업상태 (01 계속사업자, 02 휴업자, 03 폐업자)
        private String tax_type_cd;                 // 과세유형 (01 일반 02 간이? 03 면세?)
        private String tax_type_change_dt;          // 과세유형 전환일자 (YYYYMMDD)
        private String end_dt;                      // 폐업일

        public User toUserEntity(RegisterRequest request, String indNm) {
            return User.builder()
                    .id(this.b_no)
                    .stt(Status.ofCode(this.b_stt_cd))
                    .taxType(TaxType.ofCode(this.tax_type_cd))
                    .taxTypeChangeDt(parseDate(this.tax_type_change_dt))
                    .endDt(parseDate(this.end_dt))
                    .nm(request.getNm())
                    .indCd(request.getIndCd())
                    .indNm(indNm)
                    .build();
        }

        public void batchUpdate(User user) {
            user.batchUpdate(
                    Status.ofCode(this.b_stt_cd),
                    TaxType.ofCode(this.tax_type_cd),
                    parseDate(this.tax_type_change_dt),
                    parseDate(this.end_dt)
            );
        }

        public LocalDate parseDate(String date) {
            if (date == null || date.isBlank() || "null".equals(date)) {
                return null;
            }
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
    }

//    {
//        "status_code": "OK",
//            "match_cnt": 1,
//            "request_cnt": 1,
//            "data": [
//        {
//            "b_no": "0000000000",
//             "b_stt": "계속사업자",
//             "b_stt_cd": "01",
//             "tax_type": "부가가치세 일반과세자",
//             "tax_type_cd": "01",
//             "end_dt": "20000101",
//             "utcc_yn": "Y",
//             "tax_type_change_dt": "20000101",
//             "invoice_apply_dt": "20000101",
//             "rbf_tax_type": "부가가치세 일반과세자",
//             "rbf_tax_type_cd": "01"
//        }
//      ]
//    }
}
