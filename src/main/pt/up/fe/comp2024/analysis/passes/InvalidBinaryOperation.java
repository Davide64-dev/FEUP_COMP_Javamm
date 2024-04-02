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
        addVisit(Kind.BINARY_EXPR, this::visitMethodCallExpr);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMethodCallExpr(JmmNode binaryRefExpr, SymbolTable table) {

        System.out.println("Binary Expression Found");

        var operator = binaryRefExpr.getChild(1);

        var leftOperand = binaryRefExpr.getChild(0);

        var rightOperand = binaryRefExpr.getChild(2);


        // Check if the operands are nodes that return an integer
        if (ARITHMETIC_OPERATORS.contains(operator.get("name"))){
            System.out.println("Arithmetic Operation");
            System.out.println(leftOperand);
            if ((getVariableType(leftOperand, table, currentMethod).getName().equals("int") &&
                    !getVariableType(leftOperand, table, currentMethod).isArray())){
                System.out.println("Left Operand is an Integer");

                if (getVariableType(rightOperand, table, currentMethod).equals("int") &&
                        !getVariableType(leftOperand, table, currentMethod).isArray()){
                    System.out.print("Right Operand is an Integer");
                    return null;
                }
            }
        }


        // Check if the operands are both Boolean
        else if (BOOLEAN_OPERATORS.contains(operator.get("name"))){
            System.out.print("Boolean Operation");
            System.out.println(rightOperand);
            if (getVariableType(leftOperand, table, currentMethod).getName().equals("boolean")){
                System.out.println("Left Operand is a Boolean");

                if (getVariableType(rightOperand, table, currentMethod).equals("boolean")){
                    System.out.print("Right Operand is a Boolean");
                    return null;
                }
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

