package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Arrays;
import java.util.List;

public class InvalidAssign extends AnalysisVisitor {

    public static final List<String> ARITHMETIC_OPERATORS = Arrays.asList("*", "/", "-", "+");

    public static final List<String> BOOLEAN_OPERATORS = Arrays.asList("||", "&&", "<", "!");
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_ACCESS, this::assignAccess);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void assignAccess(JmmNode assignExpr, SymbolTable table) {

        System.out.println("Array Access Found");

        var arrayAccess = assignExpr.getChild(1);

        if(ARITHMETIC_OPERATORS.contains(arrayAccess.getKind().toString())){
            // It's an arithmetic operation. Already validated as true;
            return null;
        }

        if (arrayAccess.getKind().toString().equals("const")){
            if (arrayAccess.get("name") != "true" && arrayAccess.get("name") != "false"){
                // It is a constant. It is valid;
                return null;
            }
        }


        // Check if the variable is an integer variable
        for (var param : table.getFields()){
            if (param.getName().equals(arrayAccess.get("name"))){
                if (param.getType().getName().equals("int") && !param.getType().isArray()){
                    return null;
                }
            }
        }

        for (var param : table.getParameters(currentMethod)){
            if (param.getName().equals(arrayAccess.get("name"))) {
                if (param.getType().getName().equals("int") && !param.getType().isArray()) {
                    return null;
                }
            }
        }

        for (var param : table.getLocalVariables(currentMethod)){
            if (param.getName().equals(arrayAccess.get("name"))) {
                if (param.getType().getName().equals("int") && !param.getType().isArray()) {
                    return null;
                }
            }
        }


        if (assignExpr.getKind().equals(Kind.METHOD_CALL.toString())){
            List<Symbol> methods =table.getFields();

            for (var method : methods){
                if (method.equals(assignExpr.get("name"))){
                    var returnType = table.getReturnType(method.getName());
                    if (!returnType.isArray() && returnType.getName().equals("int")){
                        return null;
                    }
                }
            }
        }

        var message = String.format("'%s' is not a valid array access", arrayAccess.get("name"));


        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(assignExpr),
                NodeUtils.getColumn(assignExpr),
                message,
                null)
        );

        return null;
    }
}
