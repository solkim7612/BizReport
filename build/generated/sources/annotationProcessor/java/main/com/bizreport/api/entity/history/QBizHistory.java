package com.bizreport.api.entity.history;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.bizreport.api.entity.common.TaxType;
import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBizHistory is a Querydsl query type for BizHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBizHistory extends EntityPathBase<BizHistory> {

    private static final long serialVersionUID = 735290302L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBizHistory bizHistory = new QBizHistory("bizHistory");

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath indName = createString("indName");

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final com.bizreport.api.entity.user.QUser user;

    public final EnumPath<TaxType> vatType = createEnum("vatType", TaxType.class);

    public QBizHistory(String variable) {
        this(BizHistory.class, forVariable(variable), INITS);
    }

    public QBizHistory(Path<? extends BizHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBizHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBizHistory(PathMetadata metadata, PathInits inits) {
        this(BizHistory.class, metadata, inits);
    }

    public QBizHistory(Class<? extends BizHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.bizreport.api.entity.user.QUser(forProperty("user")) : null;
    }

}

