package com.bizreport.core.entity.user;

import com.bizreport.core.entity.global.BaseEntity;
import com.bizreport.core.entity.history.BizHistory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "USERS")
public class Users extends BaseEntity {
    @Id
    @Column(name = "b_id", length = 12)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "b_stt", nullable = false)
    private Status stt;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false)
    private TaxType taxType;

    @Column(name = "tax_type_change_dt")
    private LocalDate taxTypeChangeDt;

    @Column(name = "end_dt")
    private LocalDate endDt;

    @Column(name = "nm")
    private String nm;

    @Column(name = "ind_cd", length = 10)
    private String indCd;

    @Column(name = "ind_nm")
    private String indNm;

    @Builder
    public Users(String id, Status stt, TaxType taxType, LocalDate taxTypeChangeDt, LocalDate endDt, String nm, String indCd, String indNm) {
        this.id = id;
        this.stt = stt;
        this.taxType = taxType;
        this.taxTypeChangeDt = taxTypeChangeDt;
        this.endDt = endDt;
        this.nm = nm;
        this.indCd = indCd;
        this.indNm = indNm;
    }

    public BizHistory toHistEntity(Users user) {
        return BizHistory.builder()
                .user(user)
                .stt(stt)
                .taxType(taxType)
                .taxTypeChangeDt(taxTypeChangeDt)
                .endDt(endDt)
                .build();
    }

    public void batchUpdate(Status stt, TaxType taxType, LocalDate taxTypeChangeDt, LocalDate endDt) {
        this.stt = stt;
        this.taxType = taxType;
        this.taxTypeChangeDt = taxTypeChangeDt;
        this.endDt = endDt;
    }

    public void update(String nm, String indCd, String indNm) {
        this.nm = nm;
        this.indCd = indCd;
        this.indNm = indNm;
    }
}