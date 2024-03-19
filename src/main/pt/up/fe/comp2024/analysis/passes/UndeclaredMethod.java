package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

public class UndeclaredMethod extends AnalysisVisitor {

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

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var methodRefName = methodRefExpr.get("name");

        // Var is a field, return
        if (table.getMethods().stream()
                .anyMatch(method -> method.equals(methodRefName))) {
            System.out.println("Method is Declared, return");
            return null;
        }


        // Create error report
        System.out.println("There was an error");
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
