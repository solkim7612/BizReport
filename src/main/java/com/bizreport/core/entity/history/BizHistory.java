package com.bizreport.core.entity.history;

import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.TaxType;
import com.bizreport.core.entity.user.User;
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
    @Column(name = "b_stt", nullable = false)
    private Status stt;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false)
    private TaxType taxType;

    @Column(name = "tax_type_change_dt")
    private LocalDate taxTypeChangeDt;

    @Column(name = "tax_type_end_dt")
    private LocalDate taxTypeEndDt;

    @Column(name = "end_dt")
    private LocalDate endDt;

    @Builder
    public BizHistory(Long id, User user, Status stt, TaxType taxType, LocalDate taxTypeChangeDt, LocalDate taxTypeEndDt, LocalDate endDt) {
        this.id = id;
        this.user = user;
        this.stt = stt;
        this.taxType = taxType;
        this.taxTypeChangeDt = taxTypeChangeDt;
        this.taxTypeEndDt = (taxTypeEndDt == null) ? LocalDate.of(9999, 12, 31) : taxTypeEndDt;
        this.endDt=endDt;
    }

    public void close(LocalDate taxTypeChangeDt) {
        this.taxTypeEndDt = taxTypeChangeDt.minusDays(1);
    }
}
