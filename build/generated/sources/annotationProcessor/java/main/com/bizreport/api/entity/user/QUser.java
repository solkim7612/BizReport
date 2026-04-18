package com.bizreport.api.entity.user;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.bizreport.api.entity.common.TaxType;
import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -32557365L;

    public static final QUser user = new QUser("user");

    public final com.bizreport.api.entity.QBaseEntity _super = new com.bizreport.api.entity.QBaseEntity(this);

    public final StringPath bizName = createString("bizName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath id = createString("id");

    public final StringPath indCode = createString("indCode");

    public final DatePath<java.time.LocalDate> openDate = createDate("openDate", java.time.LocalDate.class);

    public final StringPath ownerName = createString("ownerName");

    public final StringPath password = createString("password");

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final EnumPath<BizStatus> status = createEnum("status", BizStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final EnumPath<TaxType> vatType = createEnum("vatType", TaxType.class);

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

