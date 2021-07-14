package eu.contentcloud.security.abac.predicates.converters.querydsl;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;
import eu.contentcloud.abac.predicates.model.Expression;

public class QueryDslUtils {

    public static Predicate from(Expression<Boolean> abacExpr, PathBuilder<?> entityPath) {

        var queryDslExpr = abacExpr.accept(new QueryDslConverter(entityPath));
        return (Predicate) queryDslExpr;
    }

}

