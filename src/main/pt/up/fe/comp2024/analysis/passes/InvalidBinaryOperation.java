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

    public static final List<String> ARITHMETIC_OPERATORS = Arrays.asList("*", "/", "-", "+", "<");

    public static final List<String> BOOLEAN_OPERATORS = Arrays.asList("&&", "||");
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
            if ((getVariableType(leftOperand, table).getName().equals("int") &&
                    !getVariableType(leftOperand, table).isArray())){
                System.out.println("Left Operand is an Integer");

                if (getVariableType(rightOperand, table).equals("int") && !getVariableType(leftOperand, table).isArray()){
                    System.out.print("Right Operand is an Integer");
                    return null;
                }
            }
        }


        // Check if the operands are both Boolean
        else if (BOOLEAN_OPERATORS.contains(operator.get("name"))){
            System.out.print("Boolean Operation");
            System.out.println(rightOperand);
            if (getVariableType(leftOperand, table).getName().equals("boolean")){
                System.out.println("Left Operand is a Boolean");

                if (getVariableType(rightOperand, table).equals("boolean")){
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

    private Type getVariableType(JmmNode variable, SymbolTable table){
        System.out.println("Arithmetic Operation");

        // If the value is a variable
        if (variable.getKind().equals(Kind.VAR_REF_EXPR.toString())){

            if (table.getFields().stream()
                    .anyMatch(param -> param.getName().equals(variable.get("name")))) {

                for (var symbol : table.getFields()){
                    if (symbol.getName().equals(variable.get("name"))){
                        return symbol.getType();
                    }
                }
            }

            if (table.getParameters(currentMethod).stream()
                    .anyMatch(param -> param.getName().equals(variable.get("name")))) {

                for (var symbol : table.getParameters(currentMethod)){
                    if (symbol.getName().equals(variable.get("name"))){
                        return symbol.getType();
                    }
                }
                return null;
            }

            if (table.getLocalVariables(currentMethod).stream()
                    .anyMatch(varDecl -> varDecl.getName().equals(variable.get("name")))) {

                List<Symbol> symbols =table.getLocalVariables(currentMethod);

                for (var symbol : symbols){
                    if (symbol.getName().equals(variable.get("name"))){
                        return symbol.getType();
                    }
                }
            }

        }

        // If the value is const
        if (variable.getKind().equals(Kind.CONST.toString())){
            if (variable.get("name").equals("true") || variable.get("name").equals("false")){
                return new Type("boolean", false);
            }
            else{
                return new Type("int", false);
            }
        }

        // If the value is another node
        if (variable.getKind().equals(Kind.BINARY_EXPR.toString())){
            var operator = variable.getChild(1);
            if (ARITHMETIC_OPERATORS.contains(operator.get("name")) && !operator.get("name").equals("<")){
                return new Type("int", false);
            }

            else{
                return new Type("boolean", false);
            }

        }

        // If the variable is a function
        if (variable.getKind().equals(Kind.METHOD_CALL.toString())){
            List<Symbol> methods =table.getFields();

            for (var method : methods){
                if (method.equals(variable.get("name"))){
                    return table.getReturnType(method.getName());
                }
            }
        }

        return null;
    }
}

