package com.bizreport.api.entity.user;

import com.bizreport.api.dto.auth.SignUpRequest;
import com.bizreport.api.dto.auth.StatusResponse;
import com.bizreport.api.entity.common.BaseEntity;
import com.bizreport.api.entity.common.TaxType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "USER")
public class User extends BaseEntity {
    @Id
    @Column(name = "b_id")
    private String id;

    @Column(name = "pw", nullable = false)
    private String pw;

    @Column(name = "p_nm")
    private String nm;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false)
    private TaxType taxType;

    @Column(name = "tax_type_change_dt")
    private LocalDate taxTypeChangeDt;

    @Column(name = "ind_cd")
    private String indCd;

    @Column(name = "ind_nm")
    private String indNm;

    @Enumerated(EnumType.STRING)
    @Column(name = "b_stt", nullable = false)
    private BizStatus stt;

    @Builder
    private User(String id, String pw, String nm, TaxType taxType, LocalDate taxTypeChangeDt, String indCd, String indNm, BizStatus stt) {
        this.id = id;
        this.pw = pw;
        this.nm = nm;
        this.taxType = taxType;
        this.taxTypeChangeDt = taxTypeChangeDt;
        this.indCd = indCd;
        this.indNm = indNm;
        this.stt = stt;
    }
}
