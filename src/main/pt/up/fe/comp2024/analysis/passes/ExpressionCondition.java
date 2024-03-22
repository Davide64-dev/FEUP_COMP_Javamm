package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

public class ExpressionCondition  extends AnalysisVisitor {

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

        System.out.println("Variable Found!");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var condition = stmt.getChild(0);

        if (condition.getKind().equals(Kind.CONST)){
            if (condition.get("name").equals("true") || condition.get("name").equals("false")){
                // Condition is a constant, return
                return null;
            }
        }

        if (condition.getKind().equals(Kind.BINARY_EXPR)){
            if (condition.getChild(1).get("name").equals("<") ||
                    condition.getChild(1).get("name").equals("&&") ||
                    condition.getChild(1).get("name").equals("||")){
                // Condition is a boolean expression, return
                return null;
            }
        }

        // Also need to check if it is a function call

        /*
        // Var is a field, return
        if (table.getFields().stream()
                .anyMatch(param -> param.getName().equals(condition))) {
            System.out.println("Var is a field, return");
            return null;
        }

        // Var is a parameter, return
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            System.out.println("Var is a parameter, return");
            return null;
        }

        // Var is a declared variable, return
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            System.out.println("Var is a variable, return");
            return null;
        }
        */


        // Create error report
        System.out.println("There was an error");
        var message = String.format("'%s' is not a condition.", condition.toString());


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
