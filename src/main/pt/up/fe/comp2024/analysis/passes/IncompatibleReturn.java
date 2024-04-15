package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IncompatibleReturn extends AnalysisVisitor {

    private String currentMethod;
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RET_STMT, this::visitReturn);
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitReturn(JmmNode assignExpr, SymbolTable table) {

        System.out.println("Return stmt found");

        var exprChild = assignExpr.getChild(0);
        var returnTypeActual = getVariableType(exprChild, table, currentMethod);

        var returnTypeExpected = table.getReturnType(currentMethod);

        if (returnTypeExpected.getName().equals(returnTypeActual.getName())){
            if (returnTypeExpected.isArray() == returnTypeActual.isArray()) return null;
        }

        var message = String.format("Return types are not compatible");



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
