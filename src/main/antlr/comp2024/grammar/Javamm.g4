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


INTEGER : '0' | [1-9][0-9]*;
ID : [$a-zA-Z_]+[a-zA-Z0-9_]* ;

WS : [ \t\n\r\f]+ -> skip ;

SINGLE_LINE_COM : '//' .*? '\n' -> skip ;

MULTI_LINE_COM: '/*' .*? '*/' -> skip;

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
    : type name=(MAIN | ID) SEMI
    ;

type locals[boolean isArray=false]
    : name=INT
    | name=BOOL
    | name= ID
    | name = VOID
    | name=INT LSPAREN RSPAREN {$isArray=true;}
    | name=BOOL LSPAREN RSPAREN {$isArray=true;}
    | name=ID LSPAREN RSPAREN {$isArray=true;};

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY varDecl* stmt* RCURLY
    | (PUBLIC {$isPublic=true;})? STATIC type name=MAIN LPAREN param* RPAREN LCURLY
              varDecl* stmt* RCURLY;


param
    : type (MULTIPLE)? name=ID
    ;

stmt
    : LCURLY ( stmt )* RCURLY #parStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #ifStmt
    | WHILE LPAREN expr RPAREN stmt #whileStmt
    | expr SEMI #expression
    | expr EQUALS expr SEMI #assignStmt
    | RETURN expr SEMI #retStmt
    ;

binaryOp
    : name=AND
    | name=OR
    | name=MUL
    | name=DIV
    | name=ADD
    | name=MINUS
    | name=LESS;

expr
    : LPAREN expr RPAREN #parantheses
    | NOT expr #notOp
    | expr binaryOp expr #binaryExpr
    | expr LSPAREN expr RSPAREN #arrayAccess
    | expr DOT LENGTH #objectVar
    | expr DOT name=ID LPAREN ( expr ( COMMA expr )* )? RPAREN #methodCall
    | NEW type LPAREN (expr (COMMA expr)*)? RPAREN #newArray
    | NEW type LSPAREN (expr (COMMA expr)*)? RSPAREN #newObject
    | LSPAREN (expr (COMMA expr)*)? RSPAREN #square
    | name=TRUE #const
    | name=FALSE #const
    | name=ID #varRefExpr
    | name=THIS #this
    | name=INTEGER #const
    ;





