package com.bizreport.api.entity.rate;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTaxRate is a Querydsl query type for TaxRate
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTaxRate extends EntityPathBase<TaxRate> {

    private static final long serialVersionUID = 327484480L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTaxRate taxRate = new QTaxRate("taxRate");

    public final NumberPath<java.math.BigDecimal> expRate = createNumber("expRate", java.math.BigDecimal.class);

    public final QRateId id;

    public final NumberPath<java.math.BigDecimal> vatRate = createNumber("vatRate", java.math.BigDecimal.class);

    public QTaxRate(String variable) {
        this(TaxRate.class, forVariable(variable), INITS);
    }

    public QTaxRate(Path<? extends TaxRate> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTaxRate(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTaxRate(PathMetadata metadata, PathInits inits) {
        this(TaxRate.class, metadata, inits);
    }

    public QTaxRate(Class<? extends TaxRate> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QRateId(forProperty("id")) : null;
    }

}

