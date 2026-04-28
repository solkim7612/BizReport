package com.bizreport.api.entity.user;

import com.bizreport.api.entity.global.BaseEntity;
import com.bizreport.api.entity.global.TaxType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "USER")
public class User extends BaseEntity {
    @Id
    @Column(name = "b_id", length = 12)
    private String id; // 사업자등록번호

    @Column(name = "nm")
    private String nm;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false)
    private TaxType taxType;

    @Column(name = "tax_type_change_dt")
    private LocalDate taxTypeChangeDt;

    @Column(name = "ind_cd", length = 10)
    private String indCd;

    @Column(name = "ind_nm")
    private String indNm;

    @Column(name = "b_stt")
    private String bStt;

    @Builder
    public User(String id, String nm, TaxType taxType, LocalDate taxTypeChangeDt, String indCd, String indNm, String bStt) {
        this.id = id;
        this.nm = nm;
        this.taxType = taxType;
        this.taxTypeChangeDt = taxTypeChangeDt;
        this.indCd = indCd;
        this.indNm = indNm;
        this.bStt = bStt;
    }

    public void update(TaxType taxType, String bStt, LocalDate taxTypeChangeDt) {
        this.taxType = taxType;
        this.bStt = bStt;
        this.taxTypeChangeDt = taxTypeChangeDt;
    }
}