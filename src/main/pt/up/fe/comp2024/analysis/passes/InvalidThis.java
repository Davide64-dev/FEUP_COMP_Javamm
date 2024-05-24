package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;


public class InvalidThis extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.THIS_EXPR, this::visitThis);
    }

    private Void visitThis(JmmNode stmt, SymbolTable table) {

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        var method = stmt.getAncestor(Kind.METHOD_DECL);
        if (method.get().get("name").equals("main")) {
            var message = "This keyword cannot be used in main method";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(stmt),
                    NodeUtils.getColumn(stmt),
                    message,
                    null)
            );
        }

        if (method.isPresent() && method.get().get("isStatic").equals("true")) {
            var message = "This keyword cannot be used in static methods";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(stmt),
                    NodeUtils.getColumn(stmt),
                    message,
                    null)
            );
        }

        return null;
    }
}
