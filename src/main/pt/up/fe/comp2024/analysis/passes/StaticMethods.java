package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;


public class StaticMethods extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.THIS, this::visitThis);
        addVisit(Kind.VAR_REF_EXPR, this::visitVar);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVar(JmmNode varExpr, SymbolTable table) {
        var temp = varExpr.getAncestor(Kind.METHOD_DECL).get();
        try {
            if (temp.get("isStatic").equals("false")) {
                return null;
            }
        } catch (NullPointerException e) {
        }

        for (var field : table.getFields()) {
            if (field.getName().equals(varExpr.get("name"))) {

                for (var variable : table.getLocalVariables(currentMethod)) {
                    if (variable.getName().equals(varExpr.get("name"))) return null;
                }
                var message = "Can't use fields  on static methods";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varExpr),
                        NodeUtils.getColumn(varExpr),
                        message,
                        null)
                );

            }
        }
        return null;
    }

    private Void visitThis(JmmNode thisExpr, SymbolTable table) {

        var temp = thisExpr.getAncestor(Kind.METHOD_DECL).get();
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        try {
            if (temp.get("isStatic").equals("false")) {
                return null;
            }
        } catch (NullPointerException e) {
        }

        // Create error report
        var message = "Can't use keyword this on static methods";

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(thisExpr),
                NodeUtils.getColumn(thisExpr),
                message,
                null)
        );

        return null;
    }
}
