package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Arrays;
import java.util.List;

public class InvalidAssign extends AnalysisVisitor {

    public static final List<String> ARITHMETIC_OPERATORS = Arrays.asList("*", "/", "-", "+");

    public static final List<String> BOOLEAN_OPERATORS = Arrays.asList("||", "&&", "<", "!");
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::assignVariable);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void assignVariable(JmmNode assignExpr, SymbolTable table) {

        System.out.println("Variable Assign found!");

        var assigned = assignExpr.getChild(0);
        var assignee = assignExpr.getChild(1);

        if (!assigned.getKind().equals(Kind.VAR_REF_EXPR.toString())){
            var message = String.format("'%s' is not a variable", assigned.get("name"));

            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignExpr),
                    NodeUtils.getColumn(assignExpr),
                    message,
                    null)
            );
            return null;
        }

        if (getVariableType(assigned, table, currentMethod).getName().equals(getVariableType(assignee, table, currentMethod).getName())
                && getVariableType(assigned, table, currentMethod).isArray() == getVariableType(assignee, table, currentMethod).isArray()){
            // Same type
            return null;
        }

        Type assignedType = getVariableType(assigned, table, currentMethod);
        Type assigneeType = getVariableType(assignee, table, currentMethod);

        // It cannot be an integer or boolean - it's wrong. But if are both objects and imported, assume correct
        if (assignedType.getName().isEmpty() && assigneeType.getName().isEmpty()) {
            System.out.println("Both objects");
            return null;
        }


        var message = String.format("'%s' type do not correspond to '%s' type", assigned.get("name"), assignee.get("name"));


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
