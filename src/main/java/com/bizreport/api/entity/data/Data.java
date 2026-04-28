package com.bizreport.api.entity.data;

import com.bizreport.api.entity.global.BaseEntity;
import com.bizreport.api.entity.user.User;
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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "b_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataMethod method;

    @Column(name = "is_e", nullable = false)
    private boolean isE;

    @Column(name = "is_mod", nullable = false)
    private boolean isMod;

    @Column(name = "vendor_id", length = 12)
    private String vendorId;

    @Column(name = "trans_date", nullable = false)
    private LocalDate transDate;

    @Column(name = "net_value", nullable = false, precision = 15, scale = 0)
    private BigDecimal netValue;

    @Column(name = "vat_value", nullable = false, precision = 15, scale = 0)
    private BigDecimal vatValue;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal totalPrice;

    @Builder
    public Data(User user, DataType type, DataMethod method, boolean isE, boolean isMod,
                String vendorId, LocalDate transDate, BigDecimal netValue, BigDecimal vatValue, BigDecimal totalPrice) {
        this.user = user;
        this.type = type;
        this.method = method;
        this.isE = isE;
        this.isMod = isMod;
        this.vendorId = vendorId;
        this.transDate = transDate;
        this.netValue = netValue;
        this.vatValue = vatValue;
        this.totalPrice = totalPrice;
    }
}