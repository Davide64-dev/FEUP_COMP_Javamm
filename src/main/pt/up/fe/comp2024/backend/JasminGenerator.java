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
    private final FunctionClassMap<TreeNode, String> generators;
    List<Report> reports;
    String code;
    Method currentMethod;
    boolean needsPop = false;

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
        generators.put(ArrayOperand.class, this::generateArrayOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutFieldInstruction);
        generators.put(GetFieldInstruction.class, this::generateGetFieldInstruction);
        generators.put(CallInstruction.class, this::generateCallInstruction);
        generators.put(OpCondInstruction.class, this::generateOpCondInstruction);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCondInstruction);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOpInstruction);
        generators.put(GotoInstruction.class, this::generateGotoInstruction);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        // for debug
        System.out.println(code);
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

    private String convertImport(String path) {
        if (path.equals("this")) {
            return ollirResult.getOllirClass().getClassName();
        }

        for (String importedClass : ollirResult.getOllirClass().getImports()) {
            if (importedClass.endsWith("." + path)) {
                return importedClass.replace(".", "/");
            }
        }

        return path;
    }

    private String convertType(Type ollirType) {
        return switch (ollirType.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case VOID -> "V";
            case STRING -> "Ljava/lang/String;";
            case CLASS -> "L" + convertImport(ollirType.toString()) + ";";
            case OBJECTREF -> "L" + ((ClassType) ollirType).getName() + ";";
            case ARRAYREF -> "[" + convertType(((ArrayType) ollirType).getElementType());
            default -> throw new NotImplementedException(ollirType.getTypeOfElement());
        };
    }

    private String generateField(Field field) {
        return String.format(
                ".field public %s %s%s",
                field.getFieldName(),
                convertType(field.getFieldType()),
                NL
        );
    }

    private String generateLimitLocals() {
        int registerCount = 0;
        for (var reg : currentMethod.getVarTable().values()) {
            if (reg.getVirtualReg() > registerCount) {
                registerCount = reg.getVirtualReg();
            }
        }
        registerCount++; // add one because register numbers start at 0

        return String.format(".limit locals %s", registerCount);
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

        // var staticModifier = method.isStaticMethod() ? " static " : "";
        var staticModifier = methodName.equals("main") ? " static " : ""; // not sure which version to use

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
        code.append(TAB).append(".limit stack 20").append(NL);
        // code.append(TAB).append(".limit stack 99").append(NL);
        // code.append(TAB).append(".limit locals 99").append(NL);
        code.append(TAB).append(generateLimitLocals()).append(NL);

        for (var inst : method.getInstructions()) {
            // check for labels
            method.getLabels().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(inst))
                    .findFirst()
                    .ifPresent(entry -> code.append(String.format("%s:\n", entry.getKey())));

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
        var rhs = assign.getRhs();
        if (rhs instanceof CallInstruction) {
            var invType = ((CallInstruction) rhs).getInvocationType();
            if (invType == CallType.invokevirtual || invType == CallType.invokestatic) {
                needsPop = false;
            }
        }
        // generate code for loading what's on the right
        code.append(generators.apply(rhs));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var varType = assign.getTypeOfAssign().getTypeOfElement();
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        String inst;
        String end = reg > 3 ? " " : "_";
        if (lhs instanceof ArrayOperand) { // esta merda n funciona** (funciona agora)
            code.append(String.format(
                    "aload%s%s\n%s%s",
                    end,
                    reg,
                    generators.apply(((ArrayOperand) lhs).getIndexOperands().get(0)),
                    generators.apply(rhs)
            ));
        }

        switch (varType) {
            case INT32, BOOLEAN: {
                if (currentMethod.getVarTable().get(operand.getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                    code.append("iastore").append(NL);
                    return code.toString();
                }
                inst = String.format("istore%s", end);
                break;
            }
            default: {
                inst = String.format("astore%s", end);
                break;
            }
        }

        code.append(inst).append(reg).append(NL);

        return code.toString();
    }

    private String generatePutFieldInstruction(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();

        var value = putFieldInstruction.getValue();
        var field = putFieldInstruction.getField();

        // load "this"
        code.append(generators.apply(putFieldInstruction.getObject())).append(NL);

        // push value
        // note: other instructions other than ldc exist, that may be more
        // efficient in different situations. But I don't think that's needed here
        code.append(generators.apply(value));

        // put instruction
        // this part seems ok for now
        String className = convertImport(currentMethod.getOllirClass().getClassName());
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
        code.append(generators.apply(getFieldInstruction.getObject())).append(NL);

        String className = convertImport(getFieldInstruction.getObject().getName());
        String fieldName = field.getName();
        String fieldType = convertType(field.getType());
        String getInst = String.format("getfield %s/%s %s", className, fieldName, fieldType);
        code.append(getInst).append(NL);

        return code.toString();
    }

    private String generateCallInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();

        var invocationType = callInstruction.getInvocationType();
        var typeOfElement = callInstruction.getCaller().getType().getTypeOfElement();
        String methodClassName = switch (invocationType) {
            case invokevirtual -> convertImport(((ClassType) callInstruction.getCaller().getType()).getName());
            case NEW, invokespecial -> {
                if (typeOfElement == ElementType.THIS) {
                    yield ((ClassType) callInstruction.getCaller().getType()).getName();
                } else if (typeOfElement == ElementType.ARRAYREF) {
                    yield convertImport(callInstruction.getCaller().getType().toString());
                } else {
                    yield convertImport(((ClassType) callInstruction.getCaller().getType()).getName());
                }
            }
            case invokestatic -> {
                if (typeOfElement == ElementType.THIS) {
                    yield ((ClassType) callInstruction.getCaller().getType()).getName();
                } else {
                    yield convertImport(((Operand) callInstruction.getCaller()).getName());
                }
            }
            default -> "";
        };

        String inst;

        if (invocationType == CallType.NEW && typeOfElement != ElementType.ARRAYREF) {
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

        if (invocationType == CallType.NEW && typeOfElement == ElementType.ARRAYREF) {
            inst = "newarray int";
            code.append(loadInstructions).append(NL);
            code.append(inst).append(NL);
            return code.toString();
        }

        if (invocationType == CallType.arraylength) {
            inst = String.format(
                    "%s\n%s\n%s\n",
                    generators.apply(callInstruction.getCaller()),
                    "arraylength",
                    loadInstructions
            );
        } else {
            String methodName = switch (invocationType) {
                case invokespecial -> "<init>";
                default -> ((LiteralElement) callInstruction.getMethodName()).getLiteral().replace("\"", "");
            };
            String returnType = convertType(callInstruction.getReturnType());
            inst = String.format(
                    "%s%s %s/%s(%s)%s",
                    loadInstructions,
                    callInstruction.getInvocationType().toString(),
                    convertImport(methodClassName),
                    methodName,
                    arguments,
                    returnType
            );
        }
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

    private String generateOpCondInstruction(OpCondInstruction opCondInstruction) {
        var code = new StringBuilder();

        var inst = opCondInstruction.getCondition();
        var leftOp = ((BinaryOpInstruction) inst).getLeftOperand();
        var rightOp = ((BinaryOpInstruction) inst).getRightOperand();
        var opType = inst.getOperation().getOpType();

        // maybe merge these and change only the if
        if (opType == OperationType.LTH) {
            code.append(generators.apply(leftOp))
                    .append(generators.apply(rightOp))
                    .append("if_icmplt ").append(opCondInstruction.getLabel())
                    .append(NL);
        } else if (opType == OperationType.GTE) {
            code.append(generators.apply(leftOp))
                    .append(generators.apply(rightOp))
                    .append("if_icmpge ").append(opCondInstruction.getLabel())
                    .append(NL);
        }
        return code.toString();
    }

    private String generateSingleOpCondInstruction(SingleOpCondInstruction singleOpCondInstruction) {
        return String.format("%s\nifne %s", generators.apply(singleOpCondInstruction.getCondition()), singleOpCondInstruction.getLabel());
    }

    private String generateGotoInstruction(GotoInstruction gotoInstruction) {
        return String.format("goto %s", gotoInstruction.getLabel());
    }

    private String generateUnaryOpInstruction(UnaryOpInstruction unaryOpInstruction) {
        var code = new StringBuilder();

        if (unaryOpInstruction.getOperation().getOpType() == OperationType.NOTB) {
            code.append(String.format("iconst_1\n%sixor\n",
                    generators.apply(unaryOpInstruction.getOperand())
            ));
        }

        return code.toString();
    }

    private String generateLiteral(LiteralElement literal) {
        try {
            int value = Integer.parseInt(literal.getLiteral());
            if (value >= -1 && value <= 5) {
                return "iconst_" + value + NL;
            } else if (value >= -128 && value <= 127) {
                return "bipush " + value + NL;
            } else if (value >= -32768 && value <= 32767) {
                return "sipush " + value + NL;
            } else {
                return "ldc " + value + NL;
            }
        } catch (NumberFormatException e) {
            return "ldc " + literal.getLiteral() + NL;
        }
    }

    private String generateArrayOperand(ArrayOperand arrayOperand) {
        var reg = currentMethod.getVarTable().get(arrayOperand.getName()).getVirtualReg();

        String end = reg > 3 ? " " : "_";

        return "aload" + end + reg + NL + generators.apply(arrayOperand.getIndexOperands().get(0)) + "iaload" + NL;
    }

    private String generateOperand(Operand operand) {
        var varType = currentMethod.getVarTable().get(operand.getName()).getVarType();
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        String loadInst;
        String end = reg > 3 ? " " : "_";
        loadInst = switch (varType.getTypeOfElement()) {
            case INT32, BOOLEAN -> String.format("iload%s", end);
            default -> String.format("aload%s", end);
        };

        return String.format("%s%s\n", loadInst, reg);
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var leftOp = generators.apply(binaryOp.getLeftOperand());
        var rightOp = generators.apply(binaryOp.getRightOperand());
        var opType = binaryOp.getOperation().getOpType();

        return leftOp +
            rightOp +
            switch (opType) {
                // arithmetic
                case ADD -> "iadd";
                case MUL -> "imul";
                case SUB -> "isub";
                case DIV -> "idiv";
                // boolean
                case LTH -> "if_icmplt";
                case GTE -> "if_icmpte";
                default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
            } +
            NL;
    }

    // "optimized" version that doesn't work
    /* private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();
        String regexPattern = "iload_(\\d+)|iload (\\d+)";
        Pattern pattern = Pattern.compile(regexPattern);

        if (generators.apply(binaryOp.getRightOperand()).substring(0, generators.apply(binaryOp.getRightOperand()).length()-1).equals("bipush 1")){
            System.out.println("bipush found");
            Matcher matcher = pattern.matcher(generators.apply(binaryOp.getLeftOperand()));
            if (matcher.find()){
                return "iinc " + matcher.group(1) + " " + 1  + NL;
            }
        }

        if (generators.apply(binaryOp.getLeftOperand()).substring(0, generators.apply(binaryOp.getLeftOperand()).length()-1).equals("bipush 1")){
            System.out.println("bipsuh found");
            Matcher matcher = pattern.matcher(generators.apply(binaryOp.getRightOperand()));
            if (matcher.find()){
                return "iinc " + matcher.group(1) + NL;
            }
        }

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            // arithmetic
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB -> "isub";
            case DIV -> "idiv";
            // boolean
            // case LTH ->
            case LTH -> "if_icmplt";
            case GTE -> "if_icmpte";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    } */

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        switch (currentMethod.getReturnType().getTypeOfElement()) {
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
