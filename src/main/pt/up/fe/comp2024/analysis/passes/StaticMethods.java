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
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitThis(JmmNode thisExpr, SymbolTable table) {

        System.out.println("Variable Found!");

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        if (thisExpr.get("isStatic").equals("true")){
            return null;
        }

        // Create error report
        System.out.println("There was an error");
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
