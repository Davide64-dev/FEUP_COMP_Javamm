package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.List;

public class MethodCallParameters extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.METHOD_CALL, this::visitMethodCallExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMethodCallExpr(JmmNode methodRefExpr, SymbolTable table) {

        System.out.println("Method Found");

        var className = methodRefExpr.getAncestor(Kind.CLASS_DECL).get().get("name");

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var methodRefName = methodRefExpr.get("name");

        List<Symbol> methodParams;
        try {
            methodParams = table.getParameters(methodRefName);
        } catch(NullPointerException e){ return null;}

        var methods = table.getMethods();

        boolean inTable = false;
        for (var method : methods){
            if (method.equals(methodRefName)) inTable = true;
        }

        if (!inTable) return null;

        var callParams = methodRefExpr.getChildren(Kind.PARAM);


        if (methodParams.size() == 0 && callParams.size() == 0){
            // none has 1 parameter only
            return null;
        }

        try {
            if (methodParams.get(methodParams.size() - 1).getType().isArray()) {
                // ignore
                System.out.println("It has varargs");
                return null;
            }
        } catch (IndexOutOfBoundsException e){}

        if (methodParams.size() != callParams.size()){
            var message = "The number of parameters are not the same";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodRefExpr),
                    NodeUtils.getColumn(methodRefExpr),
                    message,
                    null)
            );
            return null;
        }

        for (int i = 0; i < methodParams.size(); i++){
            var expected = methodParams.get(i).getType();
            var actual = getVariableType(callParams.get(i), table, currentMethod);
            if (expected.getName().equals(actual.getName())){
                if (expected.isArray() == actual.isArray()){
                    // same type, must return;
                    return null;
                }
            }
        }



        // Create error report
        System.out.println("There was an error");
        var message = String.format("Argumets do are not compatible", methodRefName);



        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(methodRefExpr),
                NodeUtils.getColumn(methodRefExpr),
                message,
                null)
        );



        return null;
    }
}