package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class RepeatedNames extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.PROGRAM, this::visitProgram);
    }


    private Void visitProgram(JmmNode program, SymbolTable table) {

        // field validation;

        var fields = table.getFields();

        for (int i = 0; i < fields.size(); i++){
            for (int j = i + 1; j < fields.size();j++){
                if (fields.get(i).getName().equals(fields.get(j).getName())){

                    var message = String.format("Variable '%s' declared more than 1 time", fields.get(i).getName());

                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(program),
                            NodeUtils.getColumn(program),
                            message,
                            null)
                    );
                }
            }
        }

        var methods = table.getMethods();
        for (var i = 0; i < methods.size(); i++){
            for (var j = i + 1; j < methods.size(); j++){
                if (methods.get(i).equals(methods.get(j))) {
                    var message = String.format("Method '%s' declared more than 1 time", methods.get(i));

                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(program),
                            NodeUtils.getColumn(program),
                            message,
                            null)
                    );
                }
            }

            var method = methods.get(i);

            var locals = table.getLocalVariables(method);

            for (var j = 0; j < locals.size(); j++){
                for (var k = j + 1; k < locals.size(); k++){
                    if (locals.get(j).getName().equals(locals.get(k).getName())){
                        var message = String.format("Local Variable '%s' declared more than 1 time", locals.get(i).getName());

                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(program),
                                NodeUtils.getColumn(program),
                                message,
                                null)
                        );
                    }
                }
            }
        }

        return null;

    }
}

