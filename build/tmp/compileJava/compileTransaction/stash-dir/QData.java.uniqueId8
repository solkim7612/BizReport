package com.bizreport.api.entity.data;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QData is a Querydsl query type for Data
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QData extends EntityPathBase<Data> {

    private static final long serialVersionUID = -735983061L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QData data = new QData("data");

    public final com.bizreport.api.entity.QBaseEntity _super = new com.bizreport.api.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isE = createBoolean("isE");

    public final BooleanPath isMod = createBoolean("isMod");

    public final EnumPath<DataMethod> method = createEnum("method", DataMethod.class);

    public final NumberPath<java.math.BigDecimal> netValue = createNumber("netValue", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalPrice = createNumber("totalPrice", java.math.BigDecimal.class);

    public final DatePath<java.time.LocalDate> transDate = createDate("transDate", java.time.LocalDate.class);

    public final EnumPath<DataType> type = createEnum("type", DataType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath url = createString("url");

    public final com.bizreport.api.entity.user.QUser user;

    public final NumberPath<java.math.BigDecimal> vatValue = createNumber("vatValue", java.math.BigDecimal.class);

    public final StringPath vendorId = createString("vendorId");

    public QData(String variable) {
        this(Data.class, forVariable(variable), INITS);
    }

    public QData(Path<? extends Data> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QData(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QData(PathMetadata metadata, PathInits inits) {
        this(Data.class, metadata, inits);
    }

    public QData(Class<? extends Data> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.bizreport.api.entity.user.QUser(forProperty("user")) : null;
    }

}

