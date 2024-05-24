package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
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

    public static final List<String> ARITHMETIC_OPERATORS = Arrays.asList("*", "/", "-", "+", "<");
    public static final List<String> BOOLEAN_OPERATORS = Arrays.asList("&&", "||", "!");
    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";
    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }

    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RET_STMT, this::visitRetStmt);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(IMPORT_DECLARATION, this::visitImpDecl);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(METHOD_CALL, this::visitMethodCall);
        addVisit(NEW_OBJECT, this::visitNewObject);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(PAR_STMT, this::visitParStmt);
        addVisit(NEW_ARRAY, this::visitNewArray);
        setDefaultVisit(this::defaultVisit);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitParStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var par = visit(node.getChild(0));
        code.append(par);

        return code.toString();
    }


    private String visitNewArray(JmmNode node, Void unused) {
        return "";
    }


    private String visitArrayAccess(JmmNode node, Void unused) {

        return "";
    }

    private String visitArrayCall(JmmNode node, Void unused) {


        return "";
    }


    private String visitNewObject(JmmNode node, Void s) {
        //var type = node.getChild(0).get("name");
        var temp = exprVisitor.visit(node.getJmmChild(0));
        return temp.getComputation();
    }


    private String visitMethodCall(JmmNode node, Void s) {
        StringBuilder functionCall = new StringBuilder();

        String name = node.get("name");

        String object = "this"; // Default to "this" if it's a method call on the current object

        // Check if the method call is on an object other than "this"
        if (!node.get("is_this").equals("true")) {
            object = node.getChild(0).get("name");
        }

        String invokeType;
        if (node.get("isVirtual").equals("true")) {
            invokeType = "invokevirtual";
        } else {
            invokeType = "invokestatic";
        }

        List<String> tempVars = new ArrayList<>();

        for (int i = 1; i < node.getNumChildren(); i++) {
            JmmNode argumentNode = node.getChild(i);
            var temp = exprVisitor.visit(argumentNode);
            functionCall.append(temp.getComputation());
            tempVars.add(temp.getCode());
        }
        // Append the method invocation to the functionCall StringBuilder
        functionCall.append(String.format("%s(%s, \"%s\"", invokeType, object, name));

        try {
            // Append method arguments if available
            for (int i = 1; i < node.getNumChildren(); i++) {
                JmmNode argumentNode = node.getChild(i);
                functionCall.append(",").append(tempVars.get(i - 1));
            }
        } catch (NullPointerException e) {
        }


        // Append the closing parenthesis and return type
        functionCall.append(")").append(".V;").append(NL);

        return functionCall.toString();
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


        if (node.getParent().getKind().equals(CLASS_DECL.toString())) {
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

        System.out.println(node);

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        if (node.getJmmChild(0).getKind().equals(ARRAY_ACCESS.toString())) {


            code.append(rhs.getComputation());
            System.out.println("Have to write to the array");
            code.append("$1.").append(node.getJmmChild(0).getJmmChild(0).get("name")).append("[").append(lhs.getCode()).append(".i32")
                    .append("]").append(".i32");

            code.append(SPACE);
            code.append(ASSIGN);
            code.append(SPACE);
            code.append(".i32");
            code.append(SPACE);
            code.append(rhs.getCode());
            code.append(";\n");
            return code.toString();
        }

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

    private String visitIfStmt(JmmNode node, Void unused) {
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        int labelCounter = 1;

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }
        code.append(expr.getComputation());

        String trueLabel = "if" + labelCounter;
        String falseLabel = "endif" + labelCounter++;

        code.append("if (").append(expr.getCode()).append(") goto ").append(trueLabel).append(";\n");
        var elsecode = visit(node.getJmmChild(2));
        code.append(elsecode);
        code.append("goto ").append(falseLabel).append(";\n");
        code.append(trueLabel).append(":\n");
        var ifcode = visit(node.getChild(1));
        code.append(ifcode);
        code.append(falseLabel).append(":\n");

        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        //Passes the test but i should show whats inside the while as well
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();
        var expr = OllirExprResult.EMPTY;

        String conditionlabel = "whileCond";
        String trueLabel = "WhileLoop";
        String falseLabel = "WhileEnd";

        code.append(conditionlabel).append(":\n");

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }
        code.append(expr.getComputation());

        code.append("if (").append(expr.getCode()).append(") goto ").append(trueLabel).append(";\n");
        code.append("goto ").append(falseLabel).append(";\n");

        code.append(trueLabel).append(":\n");
        if (node.getNumChildren() > 1) {
            code.append(visit(node.getJmmChild(1), unused));
        }
        code.append("goto ").append(conditionlabel).append(";\n");
        code.append(falseLabel).append(":\n");

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var isVarArg = node.get("isVarArg");
        var isArray = node.getChild(0).get("isArray");
        var id = node.get("name");
        StringBuilder code = new StringBuilder();

        code.append(id);
        if (isVarArg == "true") {
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

        if (node.get("isStatic").equals("true")) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        var paramCode = visit(node.getJmmChild(1));
        List<JmmNode> ParamNodes = node.getChildren(PARAM);
        code.append("(");
        for (var i = 0; i < ParamNodes.size(); i++) {
            JmmNode P = ParamNodes.get(i);
            paramCode = visitParam(P, null);
            code.append(paramCode);
            if (i != ParamNodes.size() - 1) {
                code.append(", ");
            }
        }
        code.append(")");

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        var afterParam = ParamNodes.size() + 1;
        var params = node.getChildren();
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            if (!child.getKind().equals("VarDecl")) {
                if (child.getKind().equals("Expression")) {
                    var childCode = visit(child.getChild(0));
                    code.append(childCode);
                } else {
                    var childCode = visit(child);
                    code.append(childCode);
                }
            }
        }

        if (node.getChild(0).get("name").equals("void")) {
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
        } else {
            System.out.println("Not have a superclass");
            //code.append(" extends Object");
        }
        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);
            /*
            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }
            */


            code.append(result);
        }

        code.append(NL);

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

        System.out.println(code);
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
