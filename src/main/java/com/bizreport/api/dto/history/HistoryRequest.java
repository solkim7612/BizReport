package com.bizreport.api.dto.history;

import com.bizreport.api.entity.common.TaxType;
import com.bizreport.api.entity.history.BizHistory;
import com.bizreport.api.entity.user.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HistoryRequest {
    private TaxType taxType;
    private LocalDate taxTypeChangeDt;
    private String indCd;
    private String indNm;

    public BizHistory toEntity(User user) {
        return BizHistory.builder()
                .user(user)
                .taxType(this.getTaxType())
                .taxTypeChangeDt(this.getTaxTypeChangeDt())
                .taxTypeEndDt(LocalDate.of(9999, 12, 31))
                .indCd(this.getIndCd())
                .indNm(this.getIndNm())
                .build();
    }
}
