package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;


public class InvalidParameters extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        var params = method.getChildren(Kind.PARAM);
        var nParams = params.size();

        for (int i = 0; i < nParams - 1; i++) {
            var param = params.get(i);
            if (param.get("isVarArg").equals("true")) {
                var message = String.format("Varargs can only be the last parameter");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }
        }

        return null;
    }

    private Void visitMethodCallExpr(JmmNode methodRefExpr, SymbolTable table) {

        var className = methodRefExpr.getAncestor(Kind.CLASS_DECL).get().get("name");
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var methodRefName = methodRefExpr.get("name");

        // Var is a field, return
        if (table.getMethods().stream()
                .anyMatch(method -> method.equals(methodRefName))) {
            return null;
        }

        try {
            var object = methodRefExpr.getChild(0);
            var debug = getVariableType(object, table, currentMethod).getName();
            if (!getVariableType(object, table, currentMethod).getName().equals("int") && !getVariableType(object, table, currentMethod).getName().equals("boolean")
                    && !getVariableType(object, table, currentMethod).getName().equals(className)){
                // It is an object, assume method exists
                return null;
            }
            // Must check here if the object is extendable and, with that, assum it is correct
        } catch (NullPointerException e) { }

        if (!table.getSuper().isEmpty()) return null;

        // Create error report
        var message = String.format("Method '%s' does not exist.", methodRefName);

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
