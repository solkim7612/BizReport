package com.bizreport.api.entity.history;

import com.bizreport.api.dto.history.HistoryRequest;
import com.bizreport.api.entity.common.TaxType;
import com.bizreport.api.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "BIZ_HISTORY")
public class BizHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "h_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "b_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false)
    private TaxType taxType;

    @Column(name = "tax_type_change_dt", nullable = false)
    private LocalDate taxTypeChangeDt;

    @Column(name = "tax_type_end_dt", nullable = false)
    private LocalDate taxTypeEndDt;

    @Column(name = "ind_cd", nullable = false)
    private String indCd;

    @Column(name = "ind_nm")
    private String indNm;

    @Builder
    private BizHistory(User user, TaxType taxType, LocalDate taxTypeChangeDt, LocalDate taxTypeEndDt, String indCd, String indNm) {
        this.user = user;
        this.taxType = taxType;
        this.taxTypeChangeDt = taxTypeChangeDt;
        this.taxTypeEndDt = taxTypeEndDt;
        this.indCd = indCd;
        this.indNm = indNm;
    }

    public static BizHistory create(User user) {
        return BizHistory.builder()
                .user(user)
                .taxType(user.getTaxType())
                .taxTypeChangeDt(user.getTaxTypeChangeDt())
                .taxTypeEndDt(LocalDate.of(9999, 12, 31))
                .indCd(user.getIndCd())
                .indNm(user.getIndNm())
                .build();
    }

    public void end(LocalDate taxTypeChangeDt) {
        this.taxTypeEndDt = taxTypeChangeDt.minusDays(1);
    }
}
