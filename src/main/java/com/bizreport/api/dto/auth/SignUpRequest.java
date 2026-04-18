package com.bizreport.api.dto.auth;

import com.bizreport.api.entity.common.TaxType;
import com.bizreport.api.entity.history.BizHistory;
import com.bizreport.api.entity.user.BizStatus;
import com.bizreport.api.entity.user.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class SignUpRequest {
    private String id;
    private String pw;
    private String nm;
    private String indCd;
    private String indNm;

    public User toEntity(StatusResponse.StatusData data, String pw){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        LocalDate taxTypeChangeDt = (data.getTax_type_change_dt() != null && !data.getTax_type_change_dt().isEmpty())
                ? LocalDate.parse(data.getTax_type_change_dt(), formatter)
                : LocalDate.now();

        return User.builder()
                .id(data.getB_no())
                .pw(pw)
                .nm(this.getNm())
                .taxType(TaxType.parse(data.getTax_type_cd()))
                .taxTypeChangeDt(taxTypeChangeDt)
                .indCd(this.getIndCd())
                .indNm(this.getIndNm())
                .stt(BizStatus.parse(data.getB_stt_cd()))
                .build();
    }
}
