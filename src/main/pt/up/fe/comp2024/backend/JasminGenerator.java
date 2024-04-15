package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Field.class, this::generateField);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutFieldInstruction);
        generators.put(GetFieldInstruction.class, this::generateGetFieldInstruction);
        generators.put(CallInstruction.class, this::generateCallInstruction);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class public ").append(className).append(NL).append(NL); // all classes are public so this can be hard coded

        String superClassName = ollirResult.getOllirClass().getSuperClass();
        if (superClassName == null) {
            superClassName = "java/lang/Object";
        }
        code.append(String.format(".super %s", superClassName)).append(NL);

        // fields???
        code.append("; Fields").append(NL);
        for (var field : ollirResult.getOllirClass().getFields()) {
            code.append(generators.apply(field));
        }

        // generate a single constructor method
        var defaultConstructor = String.format("""
            ; Constructor
            .method public <init>()V
                aload_0
                invokespecial %s/<init>()V
                return
            .end method
            """, superClassName
        );
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }

    private String convertType(Type ollirType) {
        return switch (ollirType.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case VOID -> "V";
            case CLASS -> "L" + ollirType.toString() + ";";
            case OBJECTREF -> "L" + ollirType.toString(); // not sure this is right
            // case ARRAYREF -> "[" + ... to be implemented in the next checkpoint
            default -> throw new NotImplementedException(ollirType.getTypeOfElement());
        };
    }

    private String generateField(Field field) {
        var code = new StringBuilder();

        var fieldName = field.getFieldName();
        var fieldType = field.getFieldType();

        code.append(String.format(".field public %s %s", fieldName, convertType(fieldType))).append(NL);
        return code.toString();
    }

    private String generateMethod(Method method) {
        // set method
        currentMethod = method;

        var code = new StringBuilder();

        var methodName = method.getMethodName();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var staticModifier = method.isStaticMethod() ? " static " : "";

        // Add access modifier, static modifier and method name
        code.append("\n.method ").append(modifier).append(staticModifier).append(methodName).append("(");

        // Add parameter types
        if (methodName.equals("main")) {
            // skip calculating params, just hard code for this checkpoint
            code.append("[Ljava/lang/String;");
        } else {
            for (var param : method.getParams()) {
                code.append(convertType(param.getType()));
            }
        }
        code.append(")");

        // Add return type
        var returnType = convertType(method.getReturnType());
        code.append(returnType).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }
        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        // TODO: What is this?
        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // TODO: I think this is good and supports all possible types, but not sure
        code.append(
            switch (assign.getTypeOfAssign().getTypeOfElement()) {
                case INT32, BOOLEAN -> "istore ";
                case OBJECTREF -> "astore ";
                default -> "error ";
            }
        ).append(reg).append(NL);

        return code.toString();
    }

    private String generatePutFieldInstruction(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();

        var value = putFieldInstruction.getValue();
        var field = putFieldInstruction.getField();

        // load "this"
        code.append("aload_0").append(NL);

        // push value
        // note: other instructions other than ldc exist, that may be more
        // efficient in different situations. But I don't think that's needed here
        code.append(generateLiteral((LiteralElement) value));

        // put instruction
        // this part seems ok for now
        String className = currentMethod.getOllirClass().getClassName();
        String fieldName = field.getName();
        String fieldType = convertType(field.getType());
        String putInst = String.format("putfield %s/%s %s", className, fieldName, fieldType);
        code.append(putInst).append(NL);

        return code.toString();
    }

    private String generateGetFieldInstruction(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();

        var field = getFieldInstruction.getField();

        // load "this"
        code.append("aload_0").append(NL);

        // get instruction
        String className = currentMethod.getOllirClass().getClassName();
        String fieldName = field.getName();
        String fieldType = convertType(field.getType());
        String getInst = String.format("getfield %s/%s %s", className, fieldName, fieldType);
        code.append(getInst).append(NL);

        return code.toString();
    }

    private String generateCallInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();

        var invocationType = callInstruction.getInvocationType();
        String methodClassName =
                (invocationType == CallType.NEW || invocationType == CallType.invokespecial || invocationType == CallType.invokevirtual) ?
                ((ClassType) callInstruction.getCaller().getType()).getName() :
                ((Operand) callInstruction.getCaller()).getName(); // this should be ok now
                // ...it isn't :/
                // but maybe now it is?? I think??...
        String inst;

        if (invocationType == CallType.NEW) {
            inst = "new " + methodClassName;
            code.append(inst).append(NL);
            code.append("dup");
            code.append(NL);
            return code.toString();
        }

        if (invocationType == CallType.invokespecial || invocationType == CallType.invokevirtual) {
            System.out.println("arguments: " + callInstruction.getArguments());
            System.out.println("arguments: " + callInstruction.getOperands());
            for (var operand : callInstruction.getOperands()) {
                if (operand instanceof Operand) {

                    var reg = currentMethod.getVarTable().get(((Operand)operand).getName()).getVirtualReg();
                    System.out.println("register for ting: " + reg);
                    // code.append("aload " + reg).append(NL);
                    code.append(generateOperand((Operand) operand));
                }
            }
        }

        // get arguments
        StringBuilder arguments = new StringBuilder();
        for (var argument : callInstruction.getArguments()) {
            arguments.append(convertType(argument.getType()));

            // append load instructions to code
            // this is sort of provisional, I think
            if (invocationType != CallType.invokevirtual) {
                String op = generateOperand((Operand) argument);
                code.append(op);
            }
        }

        LiteralElement methodLiteral = (LiteralElement) callInstruction.getMethodName();
        String returnType = convertType(callInstruction.getReturnType());
        inst = String.format(
                "%s %s/%s(%s)%s",
                callInstruction.getInvocationType().toString(),
                methodClassName,
                methodLiteral.getLiteral().replace("\"", ""),
                arguments,
                returnType
        );
        code.append(inst).append(NL);

        if (invocationType == CallType.invokespecial) {
            code.append("pop").append(NL);
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var varType = currentMethod.getVarTable().get(operand.getName()).getVarType();
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        String loadInst = switch (varType.getTypeOfElement()) {
            case INT32, BOOLEAN -> "iload ";
            default -> "aload ";
        };

        return loadInst + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: not sure if all types are correct/covered
        switch(currentMethod.getReturnType().getTypeOfElement()) {
            case VOID:
                code.append("return").append(NL);
                break;
            case OBJECTREF:
                code.append(generators.apply(returnInst.getOperand()));
                code.append("areturn").append(NL);
            default:
                code.append(generators.apply(returnInst.getOperand()));
                code.append("ireturn").append(NL);
                break;
        }

        return code.toString();
    }

}
