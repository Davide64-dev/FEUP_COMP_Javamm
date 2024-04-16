package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(CONST, this::visitConst);
        addVisit(METHOD_CALL, this::visitMethodCall);
        addVisit(NEW_OBJECT, this::visitNewObject);
   
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitNewObject(JmmNode node, Void unused){

        var typeName = node.getChild(0).get("name");

        var tempVar = OptUtils.getTemp();

        StringBuilder computation = new StringBuilder();

        computation.append(tempVar).append(".").append(typeName).append(SPACE)
                        .append(ASSIGN).append(".").append(typeName).append(" new(").append(typeName).append(")")
                        .append(".").append(typeName).append(";\n");

        computation.append("invokespecial(").append(tempVar).append(".").append(typeName).append(",\"<init>\").V;\n");
        var code = tempVar + "." + typeName;
        return new OllirExprResult(code,computation);
        // temp_2.Simple :=.Simple new(Simple).Simple;
        //invokespecial(temp_2.Simple,"<init>").V;
        //s.Simple :=.Simple temp_2.Simple;

    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused){

        StringBuilder computation = new StringBuilder();

        String methodName = node.get("name");

        if (node.get("ignore_first").equals("true")){
            // normal method

            var tempVar = OptUtils.getTemp();

            var type = table.getReturnType(methodName);

            String ollirType = OptUtils.toOllirType(type);

            String code = tempVar + ollirType;

            //Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
            //String typeString = OptUtils.toOllirType(thisType);

            if (node.get("is_this").equals("true"))
                computation.append(tempVar).append(ollirType).append(SPACE).append(ASSIGN).append(SPACE).append(ollirType).append(SPACE).append("invokevirtual(this, \"" + methodName + "\")").append(ollirType).append(";\n");
                // missing arguments to pass;
            else
                computation.append(tempVar).append(ollirType).append(SPACE).append(ASSIGN).append(SPACE).append(ollirType).append(SPACE).append("invokevirtual(").append(node.getChild(0).get("name"))
                        .append(".").append("Simple").append(", \"" + methodName + "\")").append(ollirType).append(";\n");
            return new OllirExprResult(code,computation);

        }

        for (var field : table.getFields()){
            if (field.getName().equals(node.getChild(0).get("name"))) {
                computation.append("code");
                return new OllirExprResult("code");
            }
        }
        for (var varia : table.getLocalVariables(methodName)){
            if (varia.getName().equals(node.getChild(0).get("name"))) {
                computation.append("code");
                return new OllirExprResult("code");
            }
        }


        return null;
    }

    private OllirExprResult visitConst(JmmNode node, Void unused) {
        if (!node.get("name").equals("true") && !node.get("name").equals("false")) {
            var intType = new Type(TypeUtils.getIntTypeName(), false);
            String ollirIntType = OptUtils.toOllirType(intType);
            String code = node.get("name") + ollirIntType;
            return new OllirExprResult(code);
        }
        else{
            String code = node.get("name") + "boolean";
            return new OllirExprResult(code);
        }
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(2));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.getChild(1).get("name")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
