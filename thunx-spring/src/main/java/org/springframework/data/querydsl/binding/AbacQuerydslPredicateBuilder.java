package org.springframework.data.querydsl.binding;

import com.contentgrid.thunx.predicates.querydsl.FieldByReflectionAccessStrategy;
import com.contentgrid.thunx.predicates.querydsl.QueryDslConverter;
import com.contentgrid.thunx.spring.data.context.AbacContext;
import com.contentgrid.thunx.spring.data.querydsl.EntityPathResolverBasedPathBuilderFactory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Slf4j
public class AbacQuerydslPredicateBuilder {

    private final ConversionService conversionService;
    private final MultiValueBinding<Path<? extends Object>, Object> defaultBinding;
    private final Map<PathInformation, Path<?>> paths;
    private final EntityPathResolver resolver;
    private final QueryDslConverter queryDslConverter;

    public AbacQuerydslPredicateBuilder(ConversionService conversionService, EntityPathResolver resolver) {

        Assert.notNull(conversionService, "ConversionService must not be null!");

        this.defaultBinding = new QuerydslDefaultBinding();
        this.conversionService = conversionService;
        this.paths = new ConcurrentHashMap<>();
        this.resolver = resolver;

        this.queryDslConverter = new QueryDslConverter(new FieldByReflectionAccessStrategy(), new EntityPathResolverBasedPathBuilderFactory(
                resolver));
    }

    @Nullable
    public Predicate getPredicate(TypeInformation<?> type, MultiValueMap<String, String> values, QuerydslBindings bindings) {

        Assert.notNull(bindings, "Context must not be null!");

        BooleanBuilder builder = new BooleanBuilder();

        var abacContext = AbacContext.getCurrentAbacContext();
        if (abacContext != null) {
            Predicate queryDslPredicate = this.queryDslConverter.from(abacContext, type.getType());
            Assert.notNull(queryDslPredicate, "abac expression cannot be null");
            log.debug("ABAC Querydsl Predicate: {}", queryDslPredicate);

            builder.and(queryDslPredicate);
        }

        for (Entry<String, List<String>> entry : values.entrySet()) {

            if (isSingleElementCollectionWithoutText(entry.getValue())) {
                continue;
            }

            String path = entry.getKey();

            if (!bindings.isPathAvailable(path, type)) {
                continue;
            }

            PathInformation propertyPath = bindings.getPropertyPath(path, type);

            if (propertyPath == null) {
                continue;
            }

            Collection<Object> value = convertToPropertyPathSpecificType(entry.getValue(), propertyPath);
            Optional<Predicate> predicate = invokeBinding(propertyPath, bindings, value);

            predicate.ifPresent(builder::and);
        }

        return builder.getValue();
    }

    /**
     * Invokes the binding of the given values, for the given {@link PropertyPath} and {@link QuerydslBindings}.
     *
     * @param dotPath must not be {@literal null}.
     * @param bindings must not be {@literal null}.
     * @param values must not be {@literal null}.
     * @return
     */
    private Optional<Predicate> invokeBinding(PathInformation dotPath, QuerydslBindings bindings,
            Collection<Object> values) {

        Path<?> path = getPath(dotPath, bindings);

        return bindings.getBindingForPath(dotPath).orElse(defaultBinding).bind(path, values);
    }

    /**
     * Returns the {@link Path} for the given {@link PropertyPath} and {@link QuerydslBindings}. Will try to obtain the
     * {@link Path} from the bindings first but fall back to reifying it from the PropertyPath in case no specific binding
     * has been configured.
     *
     * @param path must not be {@literal null}.
     * @param bindings must not be {@literal null}.
     * @return
     */
    private Path<?> getPath(PathInformation path, QuerydslBindings bindings) {

        Optional<Path<?>> resolvedPath = bindings.getExistingPath(path);

        return resolvedPath.orElseGet(() -> paths.computeIfAbsent(path, it -> it.reifyPath(resolver)));
    }

    /**
     * Converts the given source values into a collection of elements that are of the given {@link PropertyPath}'s type.
     * Considers a single element list with an empty {@link String} an empty collection because this basically indicates
     * the property having been submitted but no value provided.
     *
     * @param source must not be {@literal null}.
     * @param path must not be {@literal null}.
     * @return
     */
    private Collection<Object> convertToPropertyPathSpecificType(List<String> source, PathInformation path) {

        Class<?> targetType = path.getLeafType();

        if (source.isEmpty() || isSingleElementCollectionWithoutText(source)) {
            return Collections.emptyList();
        }

        Collection<Object> target = new ArrayList<>(source.size());

        for (String value : source) {

            target.add(conversionService.canConvert(String.class, targetType)
                    ? conversionService.convert(value, TypeDescriptor.forObject(value), getTargetTypeDescriptor(path))
                    : value);
        }

        return target;
    }

    /**
     * Returns the target {@link TypeDescriptor} for the given {@link PathInformation} by either inspecting the field or
     * property (the latter preferred) to pick up annotations potentially defined for formatting purposes.
     *
     * @param path must not be {@literal null}.
     * @return
     */
    private static TypeDescriptor getTargetTypeDescriptor(PathInformation path) {

        PropertyDescriptor descriptor = path.getLeafPropertyDescriptor();

        Class<?> owningType = path.getLeafParentType();
        String leafProperty = path.getLeafProperty();

        TypeDescriptor result = descriptor == null //
                ? TypeDescriptor
                        .nested(org.springframework.data.util.ReflectionUtils.findRequiredField(owningType, leafProperty), 0)
                : TypeDescriptor
                        .nested(new Property(owningType, descriptor.getReadMethod(), descriptor.getWriteMethod(), leafProperty), 0);

        if (result == null) {
            throw new IllegalStateException(String.format("Could not obtain TypeDesciptor for PathInformation %s!", path));
        }

        return result;
    }

    /**
     * Returns whether the given collection has exactly one element that doesn't contain any text. This is basically an
     * indicator that a request parameter has been submitted but no value for it.
     *
     * @param source must not be {@literal null}.
     * @return
     */
    private static boolean isSingleElementCollectionWithoutText(List<String> source) {
        return source.size() == 1 && !StringUtils.hasLength(source.get(0));
    }
}
