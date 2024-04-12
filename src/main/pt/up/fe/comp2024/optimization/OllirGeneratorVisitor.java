package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

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
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(IMPORT_DECLARATION, this::visitImpDecl);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(METHOD_CALL, this::visitMethodCall);
        setDefaultVisit(this::defaultVisit);
    }




    private String visitMethodCall(JmmNode node, Void s){
        StringBuilder functionaCall = new StringBuilder();
        String name = node.get("name");
        String object = node.getChild(0).get("name");
        String argument = node.getChild(1).get("name");

        //var variables = table.getLocalVariables(node.getAncestor(METHOD_DECL).get().get("name"));
        var argumentType = ".i32";

        functionaCall.append("invokestatic(").append(object).append(", \"").append(name).append("\", ");

        functionaCall.append(argument).append(argumentType).append(")");

        functionaCall.append(".V;").append(NL);

        return functionaCall.toString();
    }

    private String visitImpDecl(JmmNode node, Void s) { //Imports
        StringBuilder importStmt = new StringBuilder();

        for (var importID : table.getImports()) {
            if (importID.contains(node.get("ID"))) {
                importStmt.append("import ");
                importStmt.append(node.get("ID"));
                importStmt.append(";");
                importStmt.append('\n');
            }
        }

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

        code.append(expr.getCode());

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
            code.append(" extends Object");
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
