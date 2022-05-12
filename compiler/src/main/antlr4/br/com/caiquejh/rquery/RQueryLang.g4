grammar RQueryLang;

query
    : conditional_expression
    ;

attribute
    : ID ('.' ID)*
    ;

value
    : (NUMBER | STRING)
    ;

list
    : value (',' value)*
    ;

conditional_expression
    : term (OP_LOGICAL term)*
    ;

term
    : expression
    | '(' expression (OP_LOGICAL expression)* ')'
    ;

expression
    : attribute OP_RELATIONAL value
    | attribute OP_LIST '(' list ')'
    | attribute OP_STRING STRING
    | attribute OP_BOOL
    | attribute OP_BETWEEN value ' and ' value
    ;

ID
    : ('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
	;

NUMBER
    : [0-9]+ ('.' [0-9]+)?
    ;

STRING
    : '\'' (.)*? '\''
    | '"' (.)*? '"'
    ;

OP_RELATIONAL
    : '>' | '<' | '>=' | '<=' | '=' | '!=' | '<>'
    ;

OP_LOGICAL
    : '&&' | '||'
    ;

OP_LIST
    : ' in' | ' not in'
    ;

OP_BOOL
    : ' is true' | ' is false'
    | ' is null' | ' is not null'
    ;

OP_STRING
    : ' contains' | ' starts'
    | ' not contains' | ' not starts'
    ;

OP_BETWEEN
    : ' between'
    ;
WS
    : [ \t\n\r] -> skip
    ;

