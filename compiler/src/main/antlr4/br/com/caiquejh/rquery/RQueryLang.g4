grammar RQueryLang;

query
    : term (op_logical term)*
    ;

attribute
    : ID (DOT ID)*
    ;

value
    : (NUMBER | STRING)
    ;

list
    : value (COMMA value)*
    ;

op_relational
    : GT | GT_EQ
    | LT | LT_EQ
    | EQ | NOT_EQ1 | NOT_EQ2
    ;

op_logical
    : AND
    | OR
    | AMP2
    | PIPE2
    ;

op_list
    : IN
    | NOT IN
    ;

op_bool
    : IS TRUE
    | IS FALSE
    | IS NULL
    | IS NOT NULL
    ;

op_string
    : CONTAINS
    | NOT CONTAINS
    | STARTS
    | NOT STARTS
    ;

op_between
    : BETWEEN
    ;

term
    : expression
    | OPEN_PAR expression (op_logical expression)* CLOSE_PAR
    ;

expression
    : attribute op_relational value
    | attribute op_list OPEN_PAR list CLOSE_PAR
    | attribute op_string STRING
    | attribute op_bool
    | attribute op_between value AND value
    ;

OPEN_PAR
    : '(';
CLOSE_PAR
    : ')';
DOT
    : '.';
COMMA
    : ',';
PIPE2
    : '||';
AMP2
    : '&&';
LT
    : '<';
LT_EQ
    : '<=';
GT
    : '>';
GT_EQ
    : '>=';
EQ
    : '=';
NOT_EQ1
    : '!=';
NOT_EQ2
    : '<>';

AND
    : A N D
    ;
BETWEEN
    : B E T W E E N
    ;
CONTAINS
    : C O N T A I N S
    ;
FALSE
    : F A L S E
    ;
IN
    : I N
    ;
IS
    : I S
    ;
NOT
    : N O T
    ;
NULL
    : N U L L
    ;
OR
    : O R
    ;
STARTS
    : S T A R T S
    ;
TRUE
    : T R U E
    ;

ID
    : [a-zA-Z_] [a-zA-Z_0-9]*
	;
NUMBER
    : [-+]? DIGIT+ ( '.' DIGIT* )? ( E [-+]? DIGIT+ )?
    ;
STRING
    : '\'' (.)*? '\''
    | '"' (.)*? '"'
    ;
WS
    : [ \t\n\r] -> skip
    ;

fragment DIGIT : [0-9];

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];
