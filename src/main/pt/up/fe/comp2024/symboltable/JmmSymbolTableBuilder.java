package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getJmmChild(root.getChildren().size() - 1);

        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        var fields = buildFields(classDecl);
        var imports = buildImports(root);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        String superClass;
        try {
            superClass = classDecl.get("superClass");
        } catch (NullPointerException e) {
            superClass = "";
        }

        return new JmmSymbolTable(fields, imports, className, superClass, methods, returnTypes, params, locals);
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        List<JmmNode> var_declarations = classDecl.getChildren(VAR_DECL);
        List<Symbol> fields = new ArrayList<Symbol>();

        for (var var_declaration : var_declarations) {
            String fieldType = var_declaration.getChild(0).get("name");
            String fieldName = var_declaration.get("name");

            Symbol field = new Symbol(new Type(fieldType, false), fieldName);
            fields.add(field);
        }

        return fields;
    }

    private static List<String> buildImports(JmmNode root) {
        int size = root.getChildren().size();
        List<String> res = new ArrayList<>();

        for (int i = 0; i < size - 1; i++) {
            res.add(root.getChild(i).get("lib"));
        }

        return res;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), new Type(method.getChild(0).get("name"), Boolean.parseBoolean(method.getChild(0).get("isArray")))));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();
        List<JmmNode> methods = classDecl.getChildren(METHOD_DECL);

        for (JmmNode method : methods) {
            var methodName = method.get("name");
            var params = new ArrayList<Symbol>();
            List<JmmNode> paramNodes = method.getChildren(PARAM);
            for (JmmNode param : paramNodes) {
                var paramName = param.get("name");
                var type = new Type(param.getChild(0).get("name"), Boolean.parseBoolean(param.getChild(0).get("isArray")) || Boolean.parseBoolean(param.get("isVarArg")));
                params.add(new Symbol(type, paramName));
            }
            map.put(methodName, params);


        }


        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {

        var vars = new ArrayList<Symbol>();
        List<JmmNode> varsDec = methodDecl.getChildren(VAR_DECL);
        for (JmmNode variable : varsDec) {
            var variableName = variable.get("name");
            var type = new Type(variable.getChild(0).get("name"), Boolean.parseBoolean(variable.getChild(0).get("isArray")));
            vars.add(new Symbol(type, variableName));
        }

        return vars;
    }

}
