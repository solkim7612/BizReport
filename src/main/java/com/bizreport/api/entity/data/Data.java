package com.bizreport.api.entity.data;

import com.bizreport.api.dto.report.DataRequest;
import com.bizreport.api.entity.common.BaseEntity;
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
    @Column(name = "data_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "b_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private DataType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_method", nullable = false)
    private DataMethod method;

    @Column(name = "is_e")
    private boolean isE;

    @Column(name = "is_mod")
    private boolean isMod;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(name = "trans_date", nullable = false)
    private LocalDate transDate;

    @Column(name = "net_value", nullable = false, precision = 15, scale = 0)
    private BigDecimal netValue;

    @Column(name = "vat_value", nullable = false, precision = 15, scale = 0)
    private BigDecimal vatValue;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal totalPrice;

    @Column(name = "ocr_url")
    private String url;

    @Builder
    private Data(User user, DataType type, DataMethod method, boolean isE, boolean isMod, String vendorId, LocalDate transDate, BigDecimal netValue, BigDecimal vatValue, BigDecimal totalPrice, String url) {
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
        this.url = url;
    }
}
