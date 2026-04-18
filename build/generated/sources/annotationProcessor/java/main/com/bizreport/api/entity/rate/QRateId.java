package com.bizreport.api.entity.rate;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRateId is a Querydsl query type for RateId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QRateId extends BeanPath<RateId> {

    private static final long serialVersionUID = 784488038L;

    public static final QRateId rateId = new QRateId("rateId");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QRateId(String variable) {
        super(RateId.class, forVariable(variable));
    }

    public QRateId(Path<? extends RateId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRateId(PathMetadata metadata) {
        super(RateId.class, metadata);
    }

}

