package br.com.caiquejh.rquery;

import br.com.caiquejh.rquery.exception.RQueryException;
import org.antlr.v4.runtime.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

/**
 * RQuery lang entry point
 *
 * @param <T> Type of entity
 * @author Caique Oliveira
 */
public class RQuery<T> {

    private static final Map<Class<?>, UnaryOperator<String>> FIELD_MAPPERS = new HashMap<>();

    private final CriteriaBuilder cb;
    private final Root<T> root;

    private UnaryOperator<String> fieldMapper;

    private RQuery(Root<T> root, CriteriaBuilder cb) {
        this.root = root;
        this.cb = cb;
        this.fieldMapper = FIELD_MAPPERS.getOrDefault(root.getJavaType(), UnaryOperator.identity());
    }

    /**
     * Parse the query string to {@link Predicate}.
     *
     * @param query the rquery
     * @return a Predicate to filter the data
     * @throws RQueryException if a syntax or semantic error occurs
     */
    public Predicate parse(String query) throws RQueryException {
        RQueryLangLexer lexer = new RQueryLangLexer(CharStreams.fromString(query));
        RQueryLangParser parser = new RQueryLangParser(new CommonTokenStream(lexer));
        CriteriaRQueryLangListener<T> listener = new CriteriaRQueryLangListener<>(cb, root, fieldMapper);
        parser.addParseListener(listener);
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RQueryException("Failed to parse at line " + line + " and at column " + charPositionInLine + " due to " + msg);
            }
        });
        parser.query();
        return listener.toPredicate();
    }

    /**
     * Add a field mapper to replace the default name.
     *
     * @param fieldMapper the field mapper
     * @return the instance this
     */
    public RQuery<T> withFieldMapper(UnaryOperator<String> fieldMapper) {
        this.fieldMapper = requireNonNull(fieldMapper, "Field mapper cannot be null");
        return this;
    }

    /**
     * Create from root and criteria builder.
     *
     * @param root the root type
     * @param cb   the criteria builder
     * @param <T>  type of entity
     * @return instance of RQuery for root
     */
    public static <T> RQuery<T> from(Root<T> root, CriteriaBuilder cb) {
        return new RQuery<>(requireNonNull(root, "Root cannot be null"),
                requireNonNull(cb, "Criteria builder cannot be null"));
    }

    /**
     * Register a value converter for a data type.
     *
     * @param classOfT  class of the type
     * @param converter a function converter
     * @param <T>       type of value
     */
    public static <T> void registerConverter(Class<T> classOfT, Function<String, T> converter) {
        ValueConverter.register(classOfT, converter);
    }

    /**
     * Register a field mapper for the entity.
     *
     * @param classOfT    class of the entity
     * @param fieldMapper a function mapper
     * @param <T>         type of entity
     */
    public static <T> void registerMapper(Class<T> classOfT, UnaryOperator<String> fieldMapper) {
        FIELD_MAPPERS.put(classOfT, fieldMapper);
    }
}
