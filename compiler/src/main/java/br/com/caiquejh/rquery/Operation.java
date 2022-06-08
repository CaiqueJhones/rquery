package br.com.caiquejh.rquery;

import java.util.stream.Stream;

enum Operation {
    EQUAL("="),
    NOT_EQUAL("!=", "<>"),
    GREATER_THAN(">"),
    GREATER_EQUAL_THAN(">="),
    LESS_THAN("<"),
    LESS_EQUAL_THAN("<="),

    IS_TRUE("is true"),
    IS_FALSE("is false"),
    IS_NULL("is null"),
    IS_NOT_NULL("is not null"),

    CONTAINS("contains"),
    NOT_CONTAINS("not contains"),
    STARTS("starts"),
    NOT_STARTS("not starts"),

    IN("in"),
    NOT_IN("not in"),

    BETWEEN("between"),

    AND("and", "&&"),
    OR("or", "||");

    private final String[] symbols;

    Operation(String... symbols) {
        this.symbols = symbols;
    }

    public static Operation fromToken(String token) {
        return Stream.of(values())
                .filter(value -> Stream.of(value.symbols).anyMatch(symbol -> symbol.equalsIgnoreCase(token)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid token for operation: " + token));
    }
}
