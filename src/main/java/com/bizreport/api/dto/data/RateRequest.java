package com.bizreport.api.dto.data;

import com.bizreport.api.entity.rate.RateId;
import com.bizreport.api.entity.rate.TaxRate;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RateRequest {
    private String year;
    private String indCd;
    private String indNm;
    private String category1;
    private String category2;
    private String category3;
    private String msg;
    private BigDecimal expRate;
    private BigDecimal overExpRate;
    private BigDecimal stndExpRate;

    public TaxRate toEntity(){
        return TaxRate.builder()
                .id(new RateId(this.indCd, this.year))
                .vatRate(categorize())
                .expRate(this.expRate)
                .build();
    }

    private BigDecimal categorize(){
        String category = (category1 + category2 + category3).replaceAll("\\s", "");

        if (containsKey(category, "소매업", "재생용", "음식점업")) return new BigDecimal("15");
        if (containsKey(category, "제조업", "농업", "임업", "어업", "소화물")) return new BigDecimal("20");
        if (containsKey(category, "숙박업")) return new BigDecimal("25");
        if (containsKey(category, "금융", "보험", "기술서비스업", "사업시설", "부동산")) return new BigDecimal("40");
        if (category.contains("기술서비스업") && category.contains("인물")) return new BigDecimal("30");

        return new BigDecimal("30");
    }

    private boolean containsKey(String target, String... keywords) {
        for (String key : keywords) {
            if (target.contains(key)) return true;
        }
        return false;
    }
}
