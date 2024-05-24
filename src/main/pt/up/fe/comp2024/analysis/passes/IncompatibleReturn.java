package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class IncompatibleReturn extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RET_STMT, this::visitReturn);
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");

        if (method.getDescendants(Kind.RET_STMT).isEmpty() && !currentMethod.equals("main")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Method has to have a return statement",
                    null)
            );
        }

        if (currentMethod.equals("main")) {
            if (method.getChild(1).getChild(0).get("name").equals("String")) {
                if (method.getChild(1).getChild(0).get("isArray").equals("true")) {
                    return null;
                }
            }
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "main method has to receive an array of strings",
                    null)
            );

            if (!method.getDescendants(Kind.THIS).isEmpty()) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Cannot use this in main method",
                        null)
                );
            }
        }

        return null;
    }

    private Void visitReturn(JmmNode assignExpr, SymbolTable table) {

        if (assignExpr.getParent().getChildren(Kind.RET_STMT).size() > 1) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignExpr),
                    NodeUtils.getColumn(assignExpr),
                    "Only one return statement is possible",
                    null)
            );
        }

        if (assignExpr.getParent().getChild(assignExpr.getParent().getChildren().size() - 1) != assignExpr) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignExpr),
                    NodeUtils.getColumn(assignExpr),
                    "Return Statement must be the last one",
                    null)
            );
        }

        var exprChild = assignExpr.getChild(0);
        var returnTypeActual = getVariableType(exprChild, table, currentMethod);

        var returnTypeExpected = table.getReturnType(currentMethod);

        if (returnTypeExpected.getName().equals(returnTypeActual.getName())) {
            if (returnTypeExpected.isArray() == returnTypeActual.isArray()) return null;
        }

        var message = "Return types are not compatible";

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
