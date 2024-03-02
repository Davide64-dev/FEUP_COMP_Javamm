grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

// Operators
EQUALS : '=';
MUL : '*' ;
ADD : '+' ;
MINUS : '-';
DIV : '/';
AND : '&&';
OR : '||';
LESS : '<';
NOT : '!';

// Utils
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LSPAREN : '[';
RSPAREN : ']';
DOT : '.';
COMMA : ',';
MULTIPLE: '...';

// Data types
INT : 'int' ;
TRUE: 'true';
FALSE: 'false';
BOOL : 'boolean';

// Class Notation
CLASS : 'class' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
EXTENDS : 'extends';
STATIC : 'static';
NEW : 'new';
VOID : 'void';
MAIN : 'main';
LENGTH : 'length';
THIS: 'this';


INTEGER : [0-9]+ ;
ID : [a-zA-Z_]+[a-zA-Z0-9_]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDeclaration* classDecl EOF
    ;

importDeclaration
    : IMPORT lib += ID ( DOT lib += ID )* SEMI;

classDecl
    : CLASS name=ID (EXTENDS superClass=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : INT
    | BOOL
    | type LSPAREN RSPAREN
    | name= ID;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY varDecl* stmt* RCURLY
    | mainMethod
    ;

mainMethod
    : STATIC VOID name=MAIN LPAREN param* RPAREN LCURLY
        varDecl* stmt* RCURLY;

param
    : type (MULTIPLE)? name=ID
    ;

stmt
    : LCURLY ( stmt )* RCURLY
    | IF LPAREN expr RPAREN stmt ELSE stmt
    | WHILE LPAREN expr RPAREN stmt
    | expr SEMI
    | expr EQUALS expr SEMI
    | RETURN expr SEMI
    ;

binaryOp
    : AND
    | OR
    | MUL
    | DIV
    | ADD
    | MINUS
    | LESS;



expr
    : LPAREN expr RPAREN
    | NOT expr
    | expr binaryOp expr
    | expr LSPAREN expr RSPAREN
    | expr DOT LENGTH
    | expr DOT ID LPAREN ( expr ( COMMA expr )* )? RPAREN
    | NEW type LPAREN (expr (COMMA expr)*)? RPAREN
    | NEW type LSPAREN (expr (COMMA expr)*)? RSPAREN
    | LSPAREN (expr (COMMA expr)*)? RSPAREN
    | INT
    | TRUE
    | FALSE
    | value=INTEGER
    | name=ID
    | THIS
    ;





