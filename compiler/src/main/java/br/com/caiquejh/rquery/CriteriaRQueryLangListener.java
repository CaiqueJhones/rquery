package br.com.caiquejh.rquery;

import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.criteria.*;
import java.util.*;
import java.util.function.UnaryOperator;

class CriteriaRQueryLangListener<T> extends RQueryLangBaseListener {

    private final CriteriaBuilder builder;

    private final UnaryOperator<String> mapField;
    private final Map<String, From<?, ?>> mappedFrom = new LinkedHashMap<>();

    private From<?, ?> from;

    private String attribute;
    private Object value;
    private List<Object> list;

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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void exitExpression(RQueryLangParser.ExpressionContext ctx) {
        if (ctx.OP_RELATIONAL() != null) {
            Path<? extends Comparable> path = from.get(attribute);
            switch (ctx.OP_RELATIONAL().getText()) {
                case "=":
                    currentPredicate = builder.equal(path, value);
                    break;
                case "<>": case "!=":
                    currentPredicate = builder.notEqual(path, value);
                    break;
                case ">":
                    currentPredicate = builder.greaterThan(path, (Comparable) value);
                    break;
                case ">=":
                    currentPredicate = builder.greaterThanOrEqualTo(path, (Comparable) value);
                    break;
                case "<":
                    currentPredicate = builder.lessThan(path, (Comparable) value);
                    break;
                case "<=":
                    currentPredicate = builder.lessThanOrEqualTo(path, (Comparable) value);
                    break;
            }
        } else if (ctx.OP_BOOL() != null) {
            switch (ctx.OP_BOOL().getText().trim()) {
                case "is true":
                    currentPredicate = builder.isTrue(from.get(attribute));
                    break;
                case "is false":
                    currentPredicate = builder.isFalse(from.get(attribute));
                    break;
                case "is null":
                    currentPredicate = builder.isNull(from.get(attribute));
                    break;
                case "is not null":
                    currentPredicate = builder.isNotNull(from.get(attribute));
                    break;
            }
        } else if (ctx.OP_STRING() != null) {
            String string = parseString(ctx.STRING().getText()).toUpperCase();
            switch (ctx.OP_STRING().getText().trim()) {
                case "contains":
                    currentPredicate = builder.like(builder.upper(from.get(attribute)), "%" + string + "%");
                    break;
                case "starts":
                    currentPredicate = builder.like(builder.upper(from.get(attribute)), string + "%");
                    break;
                case "not contains":
                    currentPredicate = builder.notLike(builder.upper(from.get(attribute)), "%" + string + "%");
                    break;
                case "not starts":
                    currentPredicate = builder.notLike(builder.upper(from.get(attribute)), string + "%");
                    break;
            }
        } else if (ctx.OP_LIST() != null) {
            switch (ctx.OP_LIST().getText().trim()) {
                case "in":
                    currentPredicate = from.get(attribute).in(list);
                    break;
                case "not in":
                    currentPredicate = builder.not(from.get(attribute).in(list));
                    break;
            }
        } else if (ctx.OP_BETWEEN() != null && "between".equals(ctx.OP_BETWEEN().getText().trim())) {
            currentPredicate = builder.between(from.get(attribute), (Comparable) list.get(list.size() - 2), (Comparable) list.get(list.size() - 1));
        }
        expressions.add(currentPredicate);
    }

    @Override
    public void enterTerm(RQueryLangParser.TermContext ctx) {
        expressionsResult = null;
    }

    @Override
    public void exitTerm(RQueryLangParser.TermContext ctx) {
        if (ObjectUtils.isNotEmpty(ctx.OP_LOGICAL())) {
            ctx.OP_LOGICAL().forEach(opLogical -> {
                switch (opLogical.getText().trim()) {
                    case "&&":
                        if (expressionsResult == null) {
                            expressionsResult = builder.and(expressions.pollFirst(), expressions.pollFirst());
                        } else {
                            expressionsResult = builder.and(expressionsResult, expressions.pollFirst());
                        }
                        break;
                    case "||":
                        if (expressionsResult == null) {
                            expressionsResult = builder.or(expressions.pollFirst(), expressions.pollFirst());
                        } else {
                            expressionsResult = builder.or(expressionsResult, expressions.pollFirst());
                        }
                        break;
                }
            });
        } else {
            expressionsResult = expressions.pollFirst();
        }
        terms.add(expressionsResult);
    }

    @Override
    public void exitConditional_expression(RQueryLangParser.Conditional_expressionContext ctx) {
        if (ObjectUtils.isNotEmpty(ctx.OP_LOGICAL())) {
            ctx.OP_LOGICAL().forEach(opLogical -> {
                switch (opLogical.getText().trim()) {
                    case "&&":
                        if (termsResult == null) {
                            termsResult = builder.and(terms.pollFirst(), terms.pollFirst());
                        } else {
                            termsResult = builder.and(termsResult, terms.pollFirst());
                        }
                        break;
                    case "||":
                        if (termsResult == null) {
                            termsResult = builder.or(terms.pollFirst(), terms.pollFirst());
                        } else {
                            termsResult = builder.or(termsResult, terms.pollFirst());
                        }
                        break;
                }
            });
        } else {
            termsResult = terms.pollFirst();
        }
    }

    public Predicate toPredicate() {
        return this.termsResult;
    }
}
