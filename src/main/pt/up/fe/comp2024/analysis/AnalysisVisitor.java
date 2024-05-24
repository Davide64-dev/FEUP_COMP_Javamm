package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2024.ast.Kind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public abstract class AnalysisVisitor extends PreorderJmmVisitor<SymbolTable, Void> implements AnalysisPass {

    public static final List<String> ARITHMETIC_OPERATORS = Arrays.asList("*", "/", "-", "+", "<");
    public static final List<String> BOOLEAN_OPERATORS = Arrays.asList("&&", "||");
    private final List<Report> reports;

    public AnalysisVisitor() {
        reports = new ArrayList<>();
        setDefaultValue(() -> null);
    }

    protected void addReport(Report report) {
        reports.add(report);
    }

    protected List<Report> getReports() {
        return reports;
    }


    @Override
    public List<Report> analyze(JmmNode root, SymbolTable table) {
        // Visit the node
        visit(root, table);

        // Return reports
        return getReports();
    }

    protected Type getVariableType(JmmNode variable, SymbolTable table, String currentMethod) {
        // If the value is a variable
        if (variable.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
            if (table.getParameters(currentMethod).stream()
                    .anyMatch(param -> param.getName().equals(variable.get("name")))) {

                for (var symbol : table.getParameters(currentMethod)) {
                    if (symbol.getName().equals(variable.get("name"))) {
                        return symbol.getType();
                    }
                }

                return null;
            }

            if (table.getLocalVariables(currentMethod).stream()
                    .anyMatch(varDecl -> varDecl.getName().equals(variable.get("name")))) {

                List<Symbol> symbols = table.getLocalVariables(currentMethod);

                for (var symbol : symbols) {
                    if (symbol.getName().equals(variable.get("name"))) {
                        return symbol.getType();
                    }
                }
            }

            if (table.getFields().stream()
                    .anyMatch(param -> param.getName().equals(variable.get("name")))) {

                for (var symbol : table.getFields()) {
                    if (symbol.getName().equals(variable.get("name"))) {
                        return symbol.getType();
                    }
                }
            }

        }

        // If the value is const
        if (variable.getKind().equals(Kind.CONST.toString())) {
            if (variable.get("name").equals("true") || variable.get("name").equals("false")) {
                return new Type("boolean", false);
            } else {
                return new Type("int", false);
            }
        }

        // If the value is another node
        if (variable.getKind().equals(Kind.BINARY_EXPR.toString())) {
            var operator = variable.get("name");
            if (ARITHMETIC_OPERATORS.contains(operator) && !operator.equals("<")) {
                return new Type("int", false);
            }
            return new Type("boolean", false);
        }

        // If the variable is a function
        if (variable.getKind().equals(Kind.METHOD_CALL.toString())) {
            var methods = table.getMethods();

            for (var method : methods) {
                if (method.equals(variable.get("name"))) {
                    return table.getReturnType(method);
                }
            }
        }

        if (variable.getKind().equals(Kind.NEW_OBJECT.toString())) {
            var type = variable.getChild(0);
            return new Type(type.get("name"), false);
        }

        if (variable.getKind().equals("ArrayAccess")) {
            return new Type("int", false);
        }

        if (variable.getKind().equals("Length")) {
            return new Type("int", false);
        }

        if (variable.getKind().equals(Kind.NEW_ARRAY.toString()) || variable.getKind().equals(Kind.ARRAY_CALL.toString())) {
            return new Type("int", true);
        }


        return new Type("", false);
    }
}
