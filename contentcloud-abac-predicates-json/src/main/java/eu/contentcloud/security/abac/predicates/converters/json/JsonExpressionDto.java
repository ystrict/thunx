package eu.contentcloud.security.abac.predicates.converters.json;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import eu.contentcloud.abac.predicates.model.Expression;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @Type(value = JsonFunctionDto.class, name = "function"),
        @Type(value = JsonScalarDto.class, name = "string"),
        @Type(value = JsonScalarDto.class, name = "number"),
        @Type(value = JsonScalarDto.class, name = "boolean"),
        @Type(value = JsonScalarDto.class, name = "null"),
        @Type(value = JsonVariableDto.class, name = "var"),
        @Type(value = JsonSymbolicReferenceDto.class, name = "ref")
})
public interface JsonExpressionDto {

    <T> Expression<T> toExpression();
}
