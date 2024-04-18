package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.JavammParser;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

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

        System.out.println("Binary Expression Found");

        var operator = binaryRefExpr.getChild(1);

        var leftOperand = binaryRefExpr.getChild(0);

        var rightOperand = binaryRefExpr.getChild(2);


        var leftType = getVariableType(leftOperand, table, currentMethod);
        var rightType = getVariableType(rightOperand, table, currentMethod);

        if (leftType.getName().equals(rightType.getName())){
            if (!leftType.isArray() && !rightType.isArray()){
                return null;
            }
        }


        // Create error report
        System.out.println("There was an error");
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

