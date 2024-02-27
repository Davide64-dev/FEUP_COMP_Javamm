grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LSPAREN : '[';
RSPAREN : ']';
MUL : '*' ;
ADD : '+' ;
MINUS : '-';
DIV : '/';
AND : '&&';
OR : '||';
LESS : '<';
NOT : '!';
VOID : 'void';
MAIN : 'main';
COMMA : ',';

CLASS : 'class' ;
INT : 'int' ;
BOOL : 'boolean';
STRING : 'String';
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import';
DOT : '.';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
EXTENDS : 'extends';
STATIC : 'static';


INTEGER : [0-9]+ ;
ID : [a-zA-Z]+[a-zA-Z0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDeclaration* classDecl EOF
    ;

importDeclaration
    : IMPORT ID ( DOT ID )* SEMI;

classDecl
    : CLASS name=ID (EXTENDS name=ID)?
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
    | STRING
    | type LSPAREN RSPAREN
    | name= ID;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param (COMMA param)* RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

mainMethod
    : STATIC VOID name=MAIN LPAREN param* RPAREN LCURLY
        varDecl* stmt* RCURLY;

param
    : type name=ID
    ;

stmt
    : LCURLY ( stmt )* RCURLY
    | IF LPAREN expr RPAREN stmt ELSE stmt
    | WHILE LPAREN expr RPAREN stmt
    | expr SEMI
    | expr EQUALS expr SEMI // #AssignStmt //
    | RETURN expr SEMI //#ReturnStmt
    ;

expr
    : expr (AND | OR) expr
    | expr (MUL | DIV) expr
    | expr (ADD | MINUS) expr
    | expr LESS expr
    | NOT expr
    | expr op= MUL expr //#BinaryExpr //
    | expr op= ADD expr //#BinaryExpr //
    | value=INTEGER //#IntegerLiteral //
    | name=ID //#VarRefExpr //
    ;



