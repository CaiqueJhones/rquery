package br.com.caiquejh.rquery;

import br.com.caiquejh.rquery.exception.RQueryException;
import br.com.caiquejh.rquery.model.*;
import org.antlr.v4.runtime.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class CriteriaRQueryLangListenerIntegrationTest {

    @BeforeEach
    @AfterEach
    void cleanUp() {
        doInTransactional(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            deleteAll(session, builder, Comment.class);
            deleteAll(session, builder, Post.class);
            deleteAll(session, builder, Author.class);
            deleteAll(session, builder, Category.class);
        });
    }

    @Test
    void shouldTestOperators() {
        Author darwin = new Author("Charles", "Darwin", null, 70, new Address("Street A", "700", true));
        Author tolkien = new Author("J. R. R.", "Tolkien", "tolkien@youmail.com", 30, new Address("Street B", "300", false));
        doInTransactional(session -> {
            session.save(darwin);
            session.save(tolkien);
        });

        /* equals */
        executeQuery("firstName = 'Charles'", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));

        /* not equals */
        executeQuery("firstName != 'Charles'", Author.class, authorQuery ->
                assertEquals(tolkien, authorQuery.getSingleResult()));

        /* greater than */
        executeQuery("age > 30", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));

        /* greater or equals than */
        executeQuery("age >= 30", Author.class, authorQuery ->
                assertIterableEquals(asList(darwin, tolkien), authorQuery.getResultList()));

        /* less than */
        executeQuery("age < 70", Author.class, authorQuery ->
                assertEquals(tolkien, authorQuery.getSingleResult()));

        /* less or equals than */
        executeQuery("age <= 70", Author.class, authorQuery ->
                assertIterableEquals(asList(darwin, tolkien), authorQuery.getResultList()));

        /* is true */
        executeQuery("address.isApartment is true", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));

        /* is false */
        executeQuery("address.isApartment is false", Author.class, authorQuery ->
                assertEquals(tolkien, authorQuery.getSingleResult()));

        /* is null */
        executeQuery("email is null", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));

        /* is false */
        executeQuery("email is not null", Author.class, authorQuery ->
                assertEquals(tolkien, authorQuery.getSingleResult()));

        /* contains */
        executeQuery("firstName contains '.'", Author.class, authorQuery ->
                assertEquals(tolkien, authorQuery.getSingleResult()));

        /* starts */
        executeQuery("firstName starts 'ch'", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));

        /* not contains */
        executeQuery("firstName not contains '.'", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));

        /* not starts */
        executeQuery("firstName not starts 'ch'", Author.class, authorQuery ->
                assertEquals(tolkien, authorQuery.getSingleResult()));

        /* between */
        executeQuery("age between 50 and 100", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));

        /* in */
        executeQuery("address.street in ('Street A', 'Street B')", Author.class, authorQuery ->
                assertIterableEquals(asList(darwin, tolkien), authorQuery.getResultList()));

        /* in */
        executeQuery("address.street not in ('Street A', 'Street B')", Author.class, authorQuery ->
                assertThrows(NoResultException.class, authorQuery::getSingleResult));
    }

    @Test
    void shouldTestConjunctionAndDisjunction() {
        Author darwin = new Author("Charles", "Darwin", null, 70, new Address("Street A", "700", true));
        Author tolkien = new Author("J. R. R.", "Tolkien", "tolkien@youmail.com", 30, new Address("Street B", "300", false));
        doInTransactional(session -> {
            session.save(darwin);
            session.save(tolkien);
        });

        executeQuery("firstName = 'Charles' && age >= 30", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));

        executeQuery("firstName = 'Charles' and age >= 30", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));

        executeQuery("(firstName = 'Charles' && age >= 30) || (firstName contains '.' && email is not null)", Author.class, authorQuery ->
                assertIterableEquals(asList(darwin, tolkien), authorQuery.getResultList()));

        executeQuery("(firstName = 'Charles' && age >= 30) or (firstName contains '.' && email is not null)", Author.class, authorQuery ->
                assertIterableEquals(asList(darwin, tolkien), authorQuery.getResultList()));

        executeQuery("firstName = 'Charles' && (age < 30 || email is null)", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));

        executeQuery("firstName = 'Charles' and (age < 30 or email is null)", Author.class, authorQuery ->
                assertEquals(darwin, authorQuery.getSingleResult()));
    }

    @Test
    void shouldTestRelationship() {
        Author authorOne = new Author("Author", "One", "a_one@mail.com", 20, new Address("Street A", "109", true));
        Author authorTwo = new Author("Author", "Two", "a_two@mail.com", 26, new Address("Street A", "109", true));
        Category category = new Category("Programming");
        Post blogPost = new Post("My blog post", category, authorOne, asList(
                new Comment("Comment 1", authorTwo),
                new Comment("Comment 2", authorOne)
        ));
        doInTransactional(session -> {
            session.save(authorOne);
            session.save(authorTwo);
            session.save(category);
            session.save(blogPost);
        });

        executeQuery("author.lastName = 'One' || comments.content = 'Comment 1'", Post.class, authorQuery ->
                assertDoesNotThrow(authorQuery::getSingleResult));

        executeQuery("comments.content = 'Comment 1'", Post.class, authorQuery ->
                assertDoesNotThrow(authorQuery::getSingleResult));

        executeQuery("comments.author.email = 'a_two@mail.com'", Post.class, authorQuery ->
                assertDoesNotThrow(authorQuery::getSingleResult));
    }

    private <T> void deleteAll(Session session, CriteriaBuilder builder, Class<T> type) {
        CriteriaDelete<T> criteriaDelete = builder.createCriteriaDelete(type);
        criteriaDelete.from(type);
        session.createQuery(criteriaDelete).executeUpdate();
    }

    private void doInTransactional(Consumer<Session> block) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            block.accept(session);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    private <T> void executeQuery(String input, Class<T> typeOfT, Consumer<Query<T>> assertions) {
        RQueryLangLexer lexer = new RQueryLangLexer(CharStreams.fromString(input));
        RQueryLangParser parser = new RQueryLangParser(new CommonTokenStream(lexer));
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RQueryException("Failed to parse at line " + line + " and at column " +  charPositionInLine +" due to " + msg, e);
            }
        });
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<T> query = builder.createQuery(typeOfT);
            Root<T> root = query.from(typeOfT);

            CriteriaRQueryLangListener<T> listener = new CriteriaRQueryLangListener<>(builder, root, UnaryOperator.identity());
            parser.addParseListener(listener);
            parser.query();

            assertions.accept(session.createQuery(query.where(listener.toPredicate())));
        }
    }
}