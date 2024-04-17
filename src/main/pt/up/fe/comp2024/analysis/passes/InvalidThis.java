package pt.up.fe.comp2024.analysis.passes;

import  pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.List;

public class InvalidThis  extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        System.out.println("Visitor Created");
        addVisit(Kind.THIS_EXPR, this::visitThis);
    }

    private Void visitThis(JmmNode stmt, SymbolTable table) {

        System.out.println("This Found!");

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        var method = stmt.getAncestor(Kind.METHOD_DECL);

        if (method.get().get("name").equals("main")){
            var message = "This keyword cannot be used in main method";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(stmt),
                    NodeUtils.getColumn(stmt),
                    message,
                    null)
            );
        }


        if (method.isPresent() && method.get().get("isStatic").equals("true")){
            var message = "This keyword cannot be used in static methods";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(stmt),
                    NodeUtils.getColumn(stmt),
                    message,
                    null)
            );
        }

        System.out.println(method);
        return null;
    }
}