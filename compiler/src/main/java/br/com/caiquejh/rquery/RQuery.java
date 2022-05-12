package br.com.caiquejh.rquery;

import br.com.caiquejh.rquery.exception.RQueryException;
import org.antlr.v4.runtime.*;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.function.UnaryOperator;

public class RQuery<T> {

    private final CriteriaBuilder cb;
    private final Root<T> root;

    private RQuery(CriteriaBuilder cb, Class<T> typeOfEntity) {
        this.cb = cb;
        this.root = cb.createQuery(typeOfEntity).from(typeOfEntity);
    }

    public Predicate parse(String query) throws RQueryException {
        var lexer = new RQueryLangLexer(CharStreams.fromString(query));
        var parser = new RQueryLangParser(new CommonTokenStream(lexer));
        var listener = new CriteriaRQueryLangListener<>(cb, root, UnaryOperator.identity());
        parser.addParseListener(listener);
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RQueryException("Failed to parse at line " + line + " and at column " +  charPositionInLine +" due to " + msg);
            }
        });
        parser.query();
        return listener.toPredicate();
    }

    public static <T> RQuery<T> from(EntityManager em, Class<T> typeOfEntity) {
        return from(em.getCriteriaBuilder(), typeOfEntity);
    }

    public static <T> RQuery<T> from(CriteriaBuilder cb, Class<T> typeOfEntity) {
        return new RQuery<>(cb, typeOfEntity);
    }
}
