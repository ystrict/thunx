package com.contentgrid.thunx.predicates.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.querydsl.core.types.dsl.PathBuilder;
import com.contentgrid.thunx.predicates.model.Comparison;
import com.contentgrid.thunx.predicates.model.LogicalOperation;
import com.contentgrid.thunx.predicates.model.Scalar;
import com.contentgrid.thunx.predicates.model.SymbolicReference;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QueryDslUtilsTest {

    static class Document {

        int security;
        String department;
    }

    @Nested
    class Operators {
        @Test
        void is_equal() {
            // document.security == 5
            var thunkExpression = Comparison.areEqual(
                    SymbolicReference.of("entity", path -> path.string("security")),
                    Scalar.of(5)
            );

            var document = new PathBuilder(Document.class, "document");

            var actual = QueryDslUtils.from(thunkExpression, document);
            assertThat(actual)
                    .isNotNull()
                    .hasToString("document.security = 5");
        }

        @Test
        void is_not_equal() {
            // document.security != 5
            var thunkExpression = Comparison.notEqual(
                    SymbolicReference.of("entity", path -> path.string("security")),
                    Scalar.of(5)
            );

            var document = new PathBuilder(Document.class, "document");

            var actual = QueryDslUtils.from(thunkExpression, document);
            assertThat(actual)
                    .isNotNull()
                    .hasToString("document.security != 5");
        }

        @Test
        void is_greater_than() {
            // document.security > 5
            var thunkExpression = Comparison.greater(
                    SymbolicReference.of("entity", path -> path.string("security")),
                    Scalar.of(5)
            );

            var document = new PathBuilder(Document.class, "document");

            var actual = QueryDslUtils.from(thunkExpression, document);
            assertThat(actual)
                    .isNotNull()
                    .hasToString("document.security > 5");
        }

        @Test
        void is_greater_or_equals() {
            // document.security >= 5
            var thunkExpression = Comparison.greaterOrEquals(
                    SymbolicReference.of("entity", path -> path.string("security")),
                    Scalar.of(5)
            );

            var document = new PathBuilder(Document.class, "document");

            var actual = QueryDslUtils.from(thunkExpression, document);
            assertThat(actual)
                    .isNotNull()
                    .hasToString("document.security >= 5");
        }

        @Test
        void is_less_than() {
            // document.security < 5
            var thunkExpression = Comparison.less(
                    SymbolicReference.of("entity", path -> path.string("security")),
                    Scalar.of(5)
            );

            var document = new PathBuilder(Document.class, "document");

            var actual = QueryDslUtils.from(thunkExpression, document);
            assertThat(actual)
                    .isNotNull()
                    .hasToString("document.security < 5");
        }

        @Test
        void is_less_or_equals() {
            // document.security <= 5
            var thunkExpression = Comparison.lessOrEquals(
                    SymbolicReference.of("entity", path -> path.string("security")),
                    Scalar.of(5)
            );

            var document = new PathBuilder(Document.class, "document");

            var actual = QueryDslUtils.from(thunkExpression, document);
            assertThat(actual)
                    .isNotNull()
                    .hasToString("document.security <= 5");
        }

        @Test
        void disjunction() {
            // document.security == 5 OR document.security == 10 OR document.security == 8
            var thunkExpression = LogicalOperation.disjunction(
                    Comparison.areEqual(
                            SymbolicReference.of("entity", path -> path.string("security")),
                            Scalar.of(5)),
                    Comparison.areEqual(
                            SymbolicReference.of("entity", path -> path.string("security")),
                            Scalar.of(10)),
                    Comparison.areEqual(
                            SymbolicReference.of("entity", path -> path.string("security")),
                            Scalar.of(8))
            );

            var document = new PathBuilder(Document.class, "document");

            var actual = QueryDslUtils.from(thunkExpression, document);
            assertThat(actual)
                    .isNotNull()
                    .hasToString("document.security = 5 || document.security = 10 || document.security = 8");

        }

        @Test
        void conjunction() {
            // document.security == 5 AND document.department == 'HR'
            var thunkExpression = LogicalOperation.conjunction(
                    Comparison.areEqual(
                            SymbolicReference.of("entity", path -> path.string("security")),
                            Scalar.of(5)),
                    Comparison.areEqual(
                            SymbolicReference.of("entity", path -> path.string("department")),
                            Scalar.of("HR"))
            );

            var document = new PathBuilder(Document.class, "document");

            var actual = QueryDslUtils.from(thunkExpression, document);
            assertThat(actual)
                    .isNotNull()
                    .hasToString("document.security = 5 && document.department = HR");

        }

    }


    @Test
    void compare_with_null() {
        // document.security == null
        var thunkExpression = Comparison.areEqual(
                SymbolicReference.of("entity", path -> path.string("security")),
                Scalar.nullValue()
        );

        var document = new PathBuilder(Document.class, "document");

        var actual = QueryDslUtils.from(thunkExpression, document);
        assertThat(actual)
                .isNotNull()
                .hasToString("document.security = null");
    }


    @Test
    void expectSubjectNotNamed_entity_throwsIllegalArgumentException() {
        var thunkExpression = Comparison.areEqual(
                SymbolicReference.of("document", path -> path.string("security")),
                Scalar.of(5)
        );

        var document = new PathBuilder(Document.class, "document");

        assertThatThrownBy(() -> QueryDslUtils.from(thunkExpression, document))
                .isInstanceOf(IllegalArgumentException.class);
    }
}