package br.com.caiquejh.rquery;

import br.com.caiquejh.rquery.exception.RQueryException;
import org.antlr.v4.runtime.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * RQuery lang entry point
 *
 * @param <T> Type of entity
 * @author Caique Oliveira
 */
public class RQuery<T> {

    private final CriteriaBuilder cb;
    private final Root<T> root;

    private RQuery(Root<T> root, CriteriaBuilder cb) {
        this.root = root;
        this.cb = cb;
    }

    public Predicate parse(String query) throws RQueryException {
        var lexer = new RQueryLangLexer(CharStreams.fromString(query));
        var parser = new RQueryLangParser(new CommonTokenStream(lexer));
        var listener = new CriteriaRQueryLangListener<>(cb, root, UnaryOperator.identity());
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
     * Create from root and criteria builder
     *
     * @param root the root type
     * @param cb   the criteria builder
     * @param <T>  type of entity
     * @return instance of RQuery for root
     */
    public static <T> RQuery<T> from(Root<T> root, CriteriaBuilder cb) {
        return new RQuery<>(root, cb);
    }

    public static <T> void registerConverter(Class<T> classOfT, Function<String, T> converter) {
        ValueConverter.register(classOfT, converter);
    }
}
