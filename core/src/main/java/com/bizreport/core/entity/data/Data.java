package com.bizreport.core.entity.data;

import com.bizreport.core.entity.global.BaseEntity;
import com.bizreport.core.entity.user.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "DATA")
public class Data extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "b_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private DataType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_method", nullable = false)
    private DataMethod method;

    @Column(name = "is_e", nullable = false)
    private boolean isE;

    @Column(name = "is_mod", nullable = false)
    private boolean isMod;

    @Column(name = "card_num", length = 20)
    private String cardNum;

    @Column(name = "vendor_id", length = 12, nullable = false)
    private String vendorId;

    @Column(name = "trans_dt", nullable = false)
    private LocalDate transDt;

    @Column(name = "net_value")
    private BigDecimal netValue;

    @Column(name = "vat_value")
    private BigDecimal vatValue;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Builder
    public Data(Users user, DataType type, DataMethod method, boolean isE, boolean isMod,
                String cardNum, String vendorId, LocalDate transDt, BigDecimal netValue, BigDecimal vatValue, BigDecimal totalPrice) {
        this.user = user;
        this.type = type;
        this.method = method;
        this.isE = isE;
        this.isMod = isMod;
        this.cardNum = cardNum;
        this.vendorId = vendorId;
        this.transDt = transDt;
        this.netValue = netValue;
        this.vatValue = vatValue;
        this.totalPrice = totalPrice;
    }
}