package com.bizreport.core.dto.business;

import com.bizreport.core.entity.history.BizHistory;
import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.TaxType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BizHistoryResponse {
    private Long id;
    private String bId;                 // 사업자등록번호 (Users의 ID)
    private Status stt;
    private String nm;
    private String indCd;
    private String indNm;
    private TaxType taxType;
    private LocalDate taxTypeChangeDt;
    private LocalDate taxTypeEndDt;

    public static BizHistoryResponse from(BizHistory history) {
        return new BizHistoryResponse(
                history.getId(),
                history.getUser().getId(),
                history.getUser().getStt(),
                history.getUser().getNm(),
                history.getUser().getIndCd(),
                history.getUser().getIndNm(),
                history.getTaxType(),
                history.getTaxTypeChangeDt(),
                history.getTaxTypeEndDt()
        );
    }
}