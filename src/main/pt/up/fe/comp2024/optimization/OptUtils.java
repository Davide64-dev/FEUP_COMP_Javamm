package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {
        TYPE.checkOrThrow(typeNode);

        String typeName = typeNode.get("name");
        boolean isArray = typeNode.get("isArray").equals("true");

        return toOllirType(typeName, isArray);
    }

    public static String toOllirType(Type type) {
        return toOllirType(type.getName(), type.isArray());
    }

    private static String toOllirType(String typeName, boolean isArray) {
        String baseType = switch (typeName) {
            case "int" -> "i32";
            case "String" -> "String";
            case "void" -> "V";
            case "boolean" -> "bool";
            default -> typeName;
        };

        String ollirType = "." + baseType;

        if (isArray) {
            ollirType = ".array" + ollirType; // Prepend `.array` to indicate it's an array
        }

        return ollirType;
    }


}
