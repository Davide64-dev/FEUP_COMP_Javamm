package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";

    public static final List<String> ARITHMETIC_OPERATORS = Arrays.asList("*", "/", "-", "+", "<");

    public static final List<String> BOOLEAN_OPERATORS = Arrays.asList("&&", "||");


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }

    protected Type getVariableType(JmmNode variable, String currentMethod){
        System.out.println("Arithmetic Operation");

        // If the value is a variable
        if (variable.getKind().equals(Kind.VAR_REF_EXPR.toString())){

            if (this.table.getFields().stream()
                    .anyMatch(param -> param.getName().equals(variable.get("name")))) {

                for (var symbol : this.table.getFields()){
                    if (symbol.getName().equals(variable.get("name"))){
                        return symbol.getType();
                    }
                }
            }

            if (this.table.getParameters(currentMethod).stream()
                    .anyMatch(param -> param.getName().equals(variable.get("name")))) {

                for (var symbol : this.table.getParameters(currentMethod)){
                    if (symbol.getName().equals(variable.get("name"))){
                        return symbol.getType();
                    }
                }
                return null;
            }

            if (this.table.getLocalVariables(currentMethod).stream()
                    .anyMatch(varDecl -> varDecl.getName().equals(variable.get("name")))) {

                List<Symbol> symbols = this.table.getLocalVariables(currentMethod);

                for (var symbol : symbols){
                    if (symbol.getName().equals(variable.get("name"))){
                        return symbol.getType();
                    }
                }
            }

        }

        // If the value is const
        if (variable.getKind().equals(Kind.CONST.toString())){
            if (variable.get("name").equals("true") || variable.get("name").equals("false")){
                return new Type("boolean", false);
            }
            else{
                return new Type("int", false);
            }
        }

        // If the value is another node
        if (variable.getKind().equals(Kind.BINARY_EXPR.toString())){
            var operator = variable.getChild(1);
            if (ARITHMETIC_OPERATORS.contains(operator.get("name")) && !operator.get("name").equals("<")){
                return new Type("int", false);
            }

            else{
                return new Type("boolean", false);
            }

        }

        // If the variable is a function
        if (variable.getKind().equals(Kind.METHOD_CALL.toString())){
            var methods =this.table.getMethods();

            for (var method : methods){
                if (method.equals(variable.get("name"))){
                    return this.table.getReturnType(method);
                }
            }
        }

        if (variable.getKind().equals(Kind.NEW_OBJECT.toString())){
            var type = variable.getChild(0);
            return new Type(type.get("name"), false);
        }


        return new Type("", false);
    }



    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RET_STMT, this::visitRetStmt);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(IMPORT_DECLARATION, this::visitImpDecl);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(METHOD_CALL, this::visitMethodCall);
        addVisit(NEW_OBJECT, this::visitNewObject);
        setDefaultVisit(this::defaultVisit);
    }


    private String visitNewObject(JmmNode node, Void s){
        //var type = node.getChild(0).get("name");
        var temp = exprVisitor.visit(node.getJmmChild(0));
        StringBuilder code = new StringBuilder();
        code.append(temp.getComputation());
        return code.toString();
    }


    private String visitMethodCall(JmmNode node, Void s) {
        StringBuilder functionaCall = new StringBuilder();
        String name = node.get("name");
        String object = node.getChild(0).get("name");

        functionaCall.append("invokestatic(").append(object).append(", \"").append(name).append("\"");

        var methodName = node.getAncestor(CLASS_DECL).get().get("name");

        try {
            for (int i = 1; i < node.getNumChildren(); i++) {
                String argument = node.getChild(i).get("name");
                var temp = exprVisitor.visit(node.getChild(i));
                // need to get argument type
                //var argumentType = this.getVariableType(node.getChild(i), methodName);

                functionaCall.append(",").append(temp.getCode());

            }
        } catch (NullPointerException e) {}

        functionaCall.append(")");

        functionaCall.append(".V;").append(NL);

        return functionaCall.toString();
    }

    private String visitImpDecl(JmmNode node, Void s) {
        StringBuilder importStmt = new StringBuilder();
        String qualifiedImport = node.get("lib")
                .replaceAll("\\[", "")
                .replaceAll("]", "")
                .replaceAll(",", ".")
                .replaceAll(" ", "");

        importStmt.append("import ").append(qualifiedImport).append(";\n");
        return importStmt.toString();
    }

    private String visitVarDecl(JmmNode node, Void s) {
        StringBuilder code = new StringBuilder();


        if (node.getParent().getKind().equals(CLASS_DECL.toString())){
            code.append(".field public ");
            var name = node.get("name");
            code.append(name);
            var retType = OptUtils.toOllirType(node.getJmmChild(0));
            code.append(retType);
            code.append(";");
            code.append(NL);
        }

        return code.toString();
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitRetStmt(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        switch (expr.getCode()) {
            case "trueboolean":
                code.append("1.bool");
                break;
            case "falseboolean":
                code.append("0.bool");
                break;
            default:
                code.append(expr.getCode());
        }

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var isArray = node.getChild(0).get("isArray");
        var id = node.get("name");
        StringBuilder code = new StringBuilder();

        code.append(id);
        if (isArray.equals("true")){
            code.append(".array");
        }

        code.append(typeCode);

        return code.toString();
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        if (node.get("isStatic").equals("true")){
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        var paramCode = visit(node.getJmmChild(1));
        List<JmmNode> ParamNodes=node.getChildren(PARAM);
        code.append("(");
        for(var i=0; i<ParamNodes.size(); i++){
            JmmNode P= ParamNodes.get(i);
            paramCode = visitParam(P,null);
            code.append(paramCode);
            if(i!=ParamNodes.size()-1){
                code.append(", ");
            }
        }
        code.append(")");

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        var afterParam = ParamNodes.size()+1;
        var params = node.getChildren();
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            if(!child.getKind().equals("VarDecl")){
                if(child.getKind().equals("Expression")){
                    var childCode = visit(child.getChild(0));
                    code.append(childCode);
                }
                else{
                    var childCode = visit(child);
                    code.append(childCode);
                }
            }
        }

        if (node.getChild(0).get("name").equals("void")){
            code.append("ret.V;\n");
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }



    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());
        //For Super Class
        var superClass = table.getSuper();
        if (!superClass.isEmpty()) {
            code.append(" extends ").append(superClass);
        }
        else{
            System.out.println("Not have a superclass");
            //code.append(" extends Object");
        }
        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);


        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        System.out.println(code.toString());
        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
