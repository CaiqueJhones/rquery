package br.com.caiquejh.rquery;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

class CriteriaRQueryLangListener<T> extends RQueryLangBaseListener {

    private final CriteriaBuilder builder;

    private final UnaryOperator<String> mapField;
    private final Map<String, From<?, ?>> mappedFrom = new LinkedHashMap<>();

    private From<?, ?> from;

    private String attribute;
    private Object value;
    private List<Object> list;

    private Operation operation;
    private List<Operation> termLogicalOperators;
    private List<Operation> queryLogicalOperators;

    private Deque<Predicate> terms;
    private Deque<Predicate> expressions;
    private Predicate termsResult;
    private Predicate expressionsResult;
    private Predicate currentPredicate;

    public CriteriaRQueryLangListener(CriteriaBuilder builder, Root<T> root, UnaryOperator<String> mapField) {
        this.builder = builder;
        this.from = root;
        this.mapField = mapField;
        this.mappedFrom.put(".", root);
    }

    @Override
    public void enterQuery(RQueryLangParser.QueryContext ctx) {
        terms = new LinkedList<>();
        expressions = new LinkedList<>();
        list = new LinkedList<>();
        termLogicalOperators = new LinkedList<>();
        queryLogicalOperators = new LinkedList<>();
    }

    @Override
    public void exitAttribute(RQueryLangParser.AttributeContext ctx) {
        String text = ctx.getText();
        text = mapField.apply(text);
        if (text.contains(".")) {
            int lastDotIndex = text.lastIndexOf('.');
            this.attribute = text.substring(lastDotIndex + 1);
            String[] attrs = text.split("\\.");
            StringBuilder acc = new StringBuilder(attrs[0]);
            for (int i = 1; i < attrs.length; i++) {
                String attr = attrs[i - 1];
                from = mappedFrom.computeIfAbsent(acc.toString(),
                        key -> mappedFrom.get(getJoinEntity(key)).join(attr));
                acc.append('.').append(attrs[i]);
            }
        } else {
            this.attribute = text;
        }
    }

    private String getJoinEntity(String key) {
        String[] split = key.split("\\.");
        return split.length <= 1 ? "." : split[split.length - 2];
    }

    @Override
    public void exitValue(RQueryLangParser.ValueContext ctx) {
        if (ctx.STRING() != null) {
            value = ValueConverter.convert(from.getJavaType(), attribute, parseString(ctx.STRING().getText()));
        } else if (ctx.NUMBER() != null) {
            value = ValueConverter.convert(from.getJavaType(), attribute, ctx.NUMBER().getText());
        }
        list.add(value);
    }

    private String parseString(String text) {
        return text.substring(1, text.length() - 1);
    }

    @Override
    public void enterList(RQueryLangParser.ListContext ctx) {
        list.clear();
    }

    @Override
    public void exitOp_relational(RQueryLangParser.Op_relationalContext ctx) {
        operation = Operation.fromToken(ctx.getText().trim());
    }

    @Override
    public void exitOp_bool(RQueryLangParser.Op_boolContext ctx) {
        operation = extractOperation(ctx);
    }

    @Override
    public void exitOp_string(RQueryLangParser.Op_stringContext ctx) {
        operation = extractOperation(ctx);
    }

    @Override
    public void exitOp_list(RQueryLangParser.Op_listContext ctx) {
        operation = extractOperation(ctx);
    }

    @Override
    public void exitOp_between(RQueryLangParser.Op_betweenContext ctx) {
        operation = Operation.fromToken(ctx.getText().trim());
    }

    @Override
    public void exitOp_logical(RQueryLangParser.Op_logicalContext ctx) {
        if (ctx.parent instanceof RQueryLangParser.TermContext) {
            termLogicalOperators.add(Operation.fromToken(ctx.getText().trim()));
        } else if (ctx.parent instanceof RQueryLangParser.QueryContext) {
            queryLogicalOperators.add(Operation.fromToken(ctx.getText().trim()));
        }
    }

    private Operation extractOperation(ParserRuleContext ctx) {
        return Operation.fromToken(ctx.children.stream()
                .map(ParseTree::getText)
                .collect(Collectors.joining(" ")));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void exitExpression(RQueryLangParser.ExpressionContext ctx) {
        String string = null;
        if (ctx.STRING() != null) {
            string = parseString(ctx.STRING().getText()).toUpperCase();
        }
        switch (operation) {
            case EQUAL:
                currentPredicate = builder.equal(from.get(attribute), value);
                break;
            case NOT_EQUAL:
                currentPredicate = builder.notEqual(from.get(attribute), value);
                break;
            case GREATER_THAN:
                currentPredicate = builder.greaterThan(from.get(attribute), (Comparable) value);
                break;
            case GREATER_EQUAL_THAN:
                currentPredicate = builder.greaterThanOrEqualTo(from.get(attribute), (Comparable) value);
                break;
            case LESS_THAN:
                currentPredicate = builder.lessThan(from.get(attribute), (Comparable) value);
                break;
            case LESS_EQUAL_THAN:
                currentPredicate = builder.lessThanOrEqualTo(from.get(attribute), (Comparable) value);
                break;
            case IS_TRUE:
                currentPredicate = builder.isTrue(from.get(attribute));
                break;
            case IS_FALSE:
                currentPredicate = builder.isFalse(from.get(attribute));
                break;
            case IS_NULL:
                currentPredicate = builder.isNull(from.get(attribute));
                break;
            case IS_NOT_NULL:
                currentPredicate = builder.isNotNull(from.get(attribute));
                break;
            case CONTAINS:
                currentPredicate = builder.like(builder.upper(from.get(attribute)),
                        "%" + string + "%");
                break;
            case STARTS:
                currentPredicate = builder.like(builder.upper(from.get(attribute)),
                        string + "%");
                break;
            case NOT_CONTAINS:
                currentPredicate = builder.notLike(builder.upper(from.get(attribute)),
                        "%" + string + "%");
                break;
            case NOT_STARTS:
                currentPredicate = builder.notLike(builder.upper(from.get(attribute)),
                        string + "%");
                break;
            case IN:
                currentPredicate = from.get(attribute).in(list);
                break;
            case NOT_IN:
                currentPredicate = builder.not(from.get(attribute).in(list));
                break;
            case BETWEEN:
                currentPredicate = builder.between(from.get(attribute),
                        (Comparable) list.get(list.size() - 2), (Comparable) list.get(list.size() - 1));
                break;
        }
        expressions.add(currentPredicate);
    }

    @Override
    public void enterTerm(RQueryLangParser.TermContext ctx) {
        expressionsResult = null;
    }

    @Override
    public void exitTerm(RQueryLangParser.TermContext ctx) {
        for (Operation operation : termLogicalOperators) {
            switch (operation) {
                case AND:
                    if (expressionsResult == null) {
                        expressionsResult = builder.and(expressions.pollFirst(), expressions.pollFirst());
                    } else {
                        expressionsResult = builder.and(expressionsResult, expressions.pollFirst());
                    }
                    break;
                case OR:
                    if (expressionsResult == null) {
                        expressionsResult = builder.or(expressions.pollFirst(), expressions.pollFirst());
                    } else {
                        expressionsResult = builder.or(expressionsResult, expressions.pollFirst());
                    }
                    break;
            }

        }
        if (termLogicalOperators.isEmpty()) {
            expressionsResult = expressions.pollFirst();
        }
        terms.add(expressionsResult);
        termLogicalOperators.clear();
    }

    @Override
    public void exitQuery(RQueryLangParser.QueryContext ctx) {
        for (Operation operation : queryLogicalOperators) {
            switch (operation) {
                case AND:
                    if (termsResult == null) {
                        termsResult = builder.and(terms.pollFirst(), terms.pollFirst());
                    } else {
                        termsResult = builder.and(termsResult, terms.pollFirst());
                    }
                    break;
                case OR:
                    if (termsResult == null) {
                        termsResult = builder.or(terms.pollFirst(), terms.pollFirst());
                    } else {
                        termsResult = builder.or(termsResult, terms.pollFirst());
                    }
                    break;
            }

        }
        if (queryLogicalOperators.isEmpty()) {
            termsResult = terms.pollFirst();
        }
        queryLogicalOperators.clear();
    }

    public Predicate toPredicate() {
        return this.termsResult;
    }
}
