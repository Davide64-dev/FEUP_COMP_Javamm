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
    boolean needsPop = false;

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

    private String getImportedClassName(String className) {
        // .this object
        if (className.equals("this"))
            return ollirResult.getOllirClass().getClassName();

        // imported object
        for (String importedClass : ollirResult.getOllirClass().getImports()) {
            if (importedClass.endsWith("." + className)) {
                return importedClass.replaceAll(".", "/");
            }
        }

        // default object name
        return className;
    }

    private String convertType(Type ollirType) {
        return switch (ollirType.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case VOID -> "V";
            case CLASS -> "L" + getImportedClassName(ollirType.toString()) + ";";
            case OBJECTREF -> "L" + getImportedClassName(((ClassType) ollirType).getName());
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
            // if an invoke virtual or invoke static instruction is being called
            // from here, it will need pop, since that means it's not in an assignment
            // (IFF it is not void, aka doesn't return anything)
            if (inst instanceof CallInstruction) {
                var invType = ((CallInstruction) inst).getInvocationType();
                if (invType == CallType.invokevirtual || invType == CallType.invokestatic) {
                    if (((CallInstruction) inst).getReturnType().getTypeOfElement() != ElementType.VOID) {
                        needsPop = true;
                    }
                }
            }
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

        // if right hand side of the expression is a call instruction,
        // and if it is an invokevirtual or static, we don't need pop,
        // since it's in an assignment
        // we will need pop if it's NOT an assignment. An example of this
        // would be:
        // foo(), where foo() returns an integer, but we're not storing anything
        // in this case we will need to pop
        // but if generateCallInstruction is being called from here, that means
        // the call instruction is part of an assignment, and therefore doesn't need pop
        if (assign.getRhs() instanceof CallInstruction) {
            var invType = ((CallInstruction) assign.getRhs()).getInvocationType();
            if (invType == CallType.invokevirtual || invType == CallType.invokestatic) {
                needsPop = false;
            }
        }
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
                default -> throw new NotImplementedException(assign.getTypeOfAssign().getTypeOfElement());
            }
        ).append(reg).append(NL);

        return code.toString();
    }

    private String generatePutFieldInstruction(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();

        var value = putFieldInstruction.getValue();
        var field = putFieldInstruction.getField();

        // load "this"
        // code.append("aload_0").append(NL);
        code.append(generators.apply(putFieldInstruction.getObject())).append(NL);

        // push value
        // note: other instructions other than ldc exist, that may be more
        // efficient in different situations. But I don't think that's needed here
        code.append(generators.apply(value));

        // put instruction
        // this part seems ok for now
        String className = getImportedClassName(currentMethod.getOllirClass().getClassName());
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
        // code.append("aload_0").append(NL);
        code.append(generators.apply(getFieldInstruction.getObject())).append(NL);

        // get instruction
        // String className = currentMethod.getOllirClass().getClassName();
//        String className = getFieldInstruction.getObject().getName().equals("this") ?
//                getImportedClassName(currentMethod.getOllirClass().getClassName()) :
//                getImportedClassName(getFieldInstruction.getObject().getName());
        String className = getImportedClassName(getFieldInstruction.getObject().getName());
        String fieldName = field.getName();
        String fieldType = convertType(field.getType());
        String getInst = String.format("getfield %s/%s %s", className, fieldName, fieldType);
        code.append(getInst).append(NL);

        return code.toString();
    }

    private String generateCallInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();

        var invocationType = callInstruction.getInvocationType();
//        String methodClassName =
//                (invocationType == CallType.NEW || invocationType == CallType.invokespecial || invocationType == CallType.invokevirtual) ?
//                ((ClassType) callInstruction.getCaller().getType()).getName() :
//                ((Operand) callInstruction.getCaller()).getName();
        String methodClassName = "";
        if (invocationType == CallType.NEW || invocationType == CallType.invokespecial) {
            methodClassName = callInstruction.getCaller().getType().getTypeOfElement() == ElementType.THIS ?
                    ((ClassType) callInstruction.getCaller().getType()).getName() :
                    getImportedClassName(((ClassType) callInstruction.getCaller().getType()).getName());
        } else if (invocationType == CallType.invokevirtual) {
            methodClassName = getImportedClassName(((ClassType) callInstruction.getCaller().getType()).getName());
        } else if (invocationType == CallType.invokestatic) {
            methodClassName = callInstruction.getCaller().getType().getTypeOfElement() == ElementType.THIS ?
                    ((ClassType) callInstruction.getCaller().getType()).getName() :
                    getImportedClassName(((Operand) callInstruction.getCaller()).getName());
        }

        String inst;

        if (invocationType == CallType.NEW) {
            inst = "new " + methodClassName;
            code.append(inst).append(NL);
            code.append("dup");
            code.append(NL);
            needsPop = true;
            return code.toString();
        }

        // get load instructions and call instruction arguments
        StringBuilder loadInstructions = new StringBuilder();
        StringBuilder arguments = new StringBuilder();
        if (invocationType == CallType.invokespecial || invocationType == CallType.invokevirtual) {
            loadInstructions.append(generators.apply(callInstruction.getCaller()));
        }
        for (var argument : callInstruction.getArguments()) {
            arguments.append(convertType(argument.getType()));

            String op = generators.apply(argument);
            loadInstructions.append(op);
        }

        LiteralElement methodLiteral = (LiteralElement) callInstruction.getMethodName();
        String returnType = convertType(callInstruction.getReturnType());
        inst = String.format(
                "%s%s %s/%s(%s)%s",
                loadInstructions,
                callInstruction.getInvocationType().toString(),
                getImportedClassName(methodClassName),
                methodLiteral.getLiteral().replace("\"", ""),
                arguments,
                returnType
        );
        code.append(inst).append(NL);

        if (needsPop) {
            code.append("pop").append(NL);
            needsPop = false;
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

        return loadInst + reg + NL; // this NL should NOT be removed
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
            case SUB -> "isub";
            case DIV -> "idiv";
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
