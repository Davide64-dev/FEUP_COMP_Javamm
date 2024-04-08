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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InvalidAssign extends AnalysisVisitor {

    public static final List<String> ARITHMETIC_OPERATORS = Arrays.asList("*", "/", "-", "+");

    public static final List<String> BOOLEAN_OPERATORS = Arrays.asList("||", "&&", "<", "!");

    private String currentMethod;
    private String superClass;
    private boolean isExtended = false;
    public List<String> imports = new ArrayList<String>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::assignVariable);
        addVisit(Kind.CLASS_DECL, this::visitClass);
        addVisit(Kind.IMPORT_DECLARATION, this::visitImport);
    }

    private Void visitImport(JmmNode declaration, SymbolTable table){

        String lib = declaration.get("lib");

        if (lib instanceof String) System.out.println("String");
        int lastIndex = lib.lastIndexOf(".");
        if (lastIndex == -1) {
            imports.add(lib.substring(1, lib.length() - 1));
            return null; // If no period found, return the original string
        } else {
            imports.add(lib.substring(lastIndex + 1)); // Return substring from last period to end
            return null;
        }
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitClass(JmmNode classDecl, SymbolTable table) {
        try {
            if (classDecl.get("superClass") != null) {
                isExtended = true;
                superClass = classDecl.get("superClass");
            }
            return null;
        }
        catch (NullPointerException e){
            return null;
        }
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

        if (getVariableType(assigned, table, currentMethod).isArray()){
            if(assignee.getKind().toString().equals((Kind.ARRAY_CALL.toString())))
                // Both arrays, return - Also check if it is an array of the same type
                // Must loop to check all the elements of the array must be integers
                //var nodeChildren = assignee.getChildren();
                for (var node : assignee.getChildren()){
                    boolean isValid = true;
                    if (!getVariableType(node, table, currentMethod).getName().equals("int")){
                        var message = String.format("'%s' type do not correspond to the correct type", assigned.get("name"));


                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(assignExpr),
                                NodeUtils.getColumn(assignExpr),
                                message,
                                null)
                        );
                    }
                }

                return null;
        }

        // If they are from the Same type
        if (getVariableType(assigned, table, currentMethod).getName().equals(getVariableType(assignee, table, currentMethod).getName())
                && getVariableType(assigned, table, currentMethod).isArray() == getVariableType(assignee, table, currentMethod).isArray()){
            // Same type
            return null;
        }

        Type assignedType = getVariableType(assigned, table, currentMethod);
        Type assigneeType = getVariableType(assignee, table, currentMethod);

        // Check if a class extends the other


        if (isExtended) {
            if (superClass.equals(assignedType.getName())) {
                System.out.println("One object extends the other");
                return null;
            }
        }

        if (imports.contains(assigneeType.getName()) && imports.contains(assignedType.getName())){
            System.out.println("Both imports, assume correct");
            return null;
        }


        var message = String.format("'%s' type do not correspond to the correct type", assigned.get("name"));


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
