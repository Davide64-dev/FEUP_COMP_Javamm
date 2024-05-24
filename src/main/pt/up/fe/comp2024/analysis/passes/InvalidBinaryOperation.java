package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class InvalidBinaryOperation extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryRefExpr, SymbolTable table) {

        var operator = binaryRefExpr.getChild(1);
        var leftOperand = binaryRefExpr.getChild(0);
        var rightOperand = binaryRefExpr.getChild(1);
        var leftType = getVariableType(leftOperand, table, currentMethod);
        var rightType = getVariableType(rightOperand, table, currentMethod);

        if (leftType.getName().equals(rightType.getName())) {
            if (!leftType.isArray() && !rightType.isArray()) {
                return null;
            }
        }

        // Create error report
        var message = String.format("Operation '%s' requires two objects of the same time", operator.get("name"));

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(binaryRefExpr),
                NodeUtils.getColumn(binaryRefExpr),
                message,
                null)
        );

        return null;
    }

}
