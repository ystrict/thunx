package eu.xenit.contentcloud.security.pbac.predicates.converters.json;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import eu.xenit.contentcloud.security.pbac.predicates.model.Comparison;
import eu.xenit.contentcloud.security.pbac.predicates.model.Scalar;
import eu.xenit.contentcloud.security.pbac.predicates.model.SymbolicReference;
import eu.xenit.contentcloud.security.pbac.predicates.model.Variable;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExpressionJsonConverterTest {

    private ExpressionJsonConverter converter = new ExpressionJsonConverter();

    @Nested
    class Scalars {

        @Test
        void string_toJson() {
            var expr = Scalar.of("foobar");
            var result = converter.toJson(expr);

            assertThatJson(result).isObject()
                    .hasSize(2)
                    .containsEntry("type", "string")
                    .containsEntry("value", "foobar");
        }

        @Test
        void intNumber_toJson() {
            var result = converter.toJson(Scalar.of(42));
            assertThatJson(result).isObject()
                    .hasSize(2)
                    .containsEntry("type", "number")
                    .containsEntry("value", BigDecimal.valueOf(42));
        }

        @Test
        void doubleNumber_toJson() {
            var expr = Scalar.of(Math.PI);
            var result = converter.toJson(expr);

            assertThatJson(result).isObject()
                    .hasSize(2)
                    .containsEntry("type", "number")
                    .containsEntry("value", BigDecimal.valueOf(3.141592653589793D));
        }

        @Test
        void boolean_toJson() {
            String jsonExprTrue = converter.toJson(Scalar.of(true));
            assertThatJson(jsonExprTrue).isObject()
                    .hasSize(2)
                    .containsEntry("type", "bool")
                    .containsEntry("value", true);

            var jsonExprFalse = converter.toJson(Scalar.of(false));
            assertThatJson(jsonExprFalse).isObject()
                    .hasSize(2)
                    .containsEntry("type", "bool")
                    .containsEntry("value", false);
        }

        @Test
        void null_toJson() {
            String result = converter.toJson(Scalar.nullValue());

            // Map.of(...) does not support null-values :facepalm:
            assertThatJson(result).isObject()
                    .hasSize(2)
                    .containsEntry("type", "null")
                    .containsEntry("value", null);

        }
    }

    @Nested
    class Functions {

        @Test
        void eq_toJson() {
            // 5 == 42
            var expr = Comparison.areEqual(Variable.named("answer"), Scalar.of(42));
            var result = converter.toJson(expr);

            assertThatJson(result)
                    .isObject()
                    .hasSize(3)
                    .containsEntry("type", "function")
                    .containsEntry("operator", "eq")
                    .hasEntrySatisfying("terms", terms -> assertThatJson(terms)
                            .isArray()
                            .hasSize(2)
                            .satisfiesExactly(
                                    e -> assertThatJson(e).isObject().containsEntry("name", "answer"),
                                    e -> assertThatJson(e).isObject().containsEntry("value", BigDecimal.valueOf(42))
                            ));
        }
    }

    @Nested
    class Variables {

        @Test
        void variable() {
            var expr = Variable.named("foo");
            var result = converter.toJson(expr);

            assertThatJson(result).isObject()
                    .hasSize(2)
                    .containsEntry("type", "var")
                    .containsEntry("name", "foo");
        }
    }

    @Nested
    class SymbolicReferences {

        @Test
        void references() {
            var expr = SymbolicReference
                    .of(Variable.named("answers"), SymbolicReference.path("life, universe and everything"));
            var result = converter.toJson(expr);
            System.out.println(result);

            assertThatJson(result).isObject()
                    .hasSize(3)
                    .containsEntry("type", "ref")
                    .hasEntrySatisfying("subject", subject -> assertThatJson(subject)
                            .isObject()
                            .hasSize(2)
                            .containsEntry("type", "var")
                            .containsEntry("name", "answers"))
                    .hasEntrySatisfying("path", path -> assertThatJson(path)
                            .isArray()
                            .hasSize(1)
                            .satisfiesExactly(e -> assertThatJson(e)
                                    .isObject()
                                    .hasSize(2)
                                    .containsEntry("type", "string")
                                    .containsEntry("value", "life, universe and everything")));


        }
    }


}