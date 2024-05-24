package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class ExpressionCondition extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IF_STMT, this::visitCondition);
        addVisit(Kind.WHILE_STMT, this::visitCondition);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitCondition(JmmNode stmt, SymbolTable table) {

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var condition = stmt.getChild(0);

        // If it is a constant - Must be true or false
        if (condition.getKind().equals(Kind.CONST.toString())) {
            if (condition.get("name").equals("true") || condition.get("name").equals("false")) {
                // Condition is a constant, return
                return null;
            }
        }

        // If it is a binary expression, it must be < && or ||
        if (condition.getKind().equals(Kind.BINARY_EXPR.toString())) {
            if (condition.get("name").equals("<") ||
                    condition.get("name").equals("&&") ||
                    condition.get("name").equals("||")) {
                // Condition is a boolean expression, return
                return null;
            }
        }

        // If it is a variable
        if (condition.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
            for (var symbol : table.getFields()) {
                if (symbol.getName().equals(condition.get("name")) && symbol.getType().getName().equals("boolean")
                        && !symbol.getType().isArray()) {
                    return null;
                }
            }

            for (var symbol : table.getParameters(currentMethod)) {
                if (symbol.getName().equals(condition.get("name")) && symbol.getType().getName().equals("boolean")
                        && !symbol.getType().isArray()) {
                    return null;
                }
            }

            for (var symbol : table.getLocalVariables(currentMethod)) {
                if (symbol.getName().equals(condition.get("name")) && symbol.getType().getName().equals("boolean")
                        && !symbol.getType().isArray()) {
                    return null;
                }
            }
        }

        if (condition.getKind().equals(Kind.METHOD_CALL.toString())) {

            for (var method : table.getFields()) {
                if (method.getName().equals(condition.get("name")) && method.getType().getName().equals("boolean")
                        && !method.getType().isArray()) {
                    return null;
                }
            }
        }

        // Create error report
        var message = String.format("'%s' is not a condition.", condition);

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(condition),
                NodeUtils.getColumn(condition),
                message,
                null)
        );

        return null;
    }
}
