package org.checkerframework.framework.stub;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.util.NumberMath;
import org.checkerframework.javacutil.ElementUtils;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * Evaluation of the constant expressions (JLS 15.29) that an annotation element value in a stub
 * file can be, and resolution of the constant names in them. Shared by {@link AnnotationFileParser}
 * and {@link BinaryStubReader}, so that the text and the binary form of a stub file give every
 * annotation the same values. Both evaluate when the stub file is read, not when the binary form is
 * written, so that a constant of another class has the value of the class on the classpath.
 *
 * <p>A constant is a {@code Boolean}, {@code Character}, {@code Byte}, {@code Short}, {@code
 * Integer}, {@code Long}, {@code Float}, {@code Double}, or {@code String}. Each operation returns
 * null where Java would not compute a constant: an operand of the wrong type, an integer division
 * by zero, or {@code ==} on strings. Operators are identified by their Java tokens, such as {@code
 * "+"} and {@code ">>>"}, which is how JavaParser's {@code asString} spells them.
 */
final class AnnotationConstants {

    /** Do not instantiate; all methods are static. */
    private AnnotationConstants() {
        throw new AssertionError("Class AnnotationConstants cannot be instantiated.");
    }

    /**
     * Returns the operand of an arithmetic operation, with a {@code Character} converted to its
     * {@code Integer} code, as numeric promotion does; or null if {@code value} is not numeric.
     *
     * @param value a constant
     * @return {@code value} as a number, or null
     */
    private static @Nullable Number numeric(Object value) {
        if (value instanceof Character) {
            return (int) (Character) value;
        }
        return value instanceof Number ? (Number) value : null;
    }

    /**
     * Returns the value of a unary operation ({@code + - ~ !}) on a constant.
     *
     * @param op the operator token
     * @param operand the operand
     * @return the result, or null if it is not a constant
     */
    static @Nullable Object unary(String op, Object operand) {
        if (op.equals("!")) {
            return operand instanceof Boolean ? !(Boolean) operand : null;
        }
        Number n = numeric(operand);
        NumberMath<?> math = n == null ? null : NumberMath.getNumberMath(n);
        if (math == null) {
            return null;
        }
        try {
            switch (op) {
                case "+":
                    return math.unaryPlus();
                case "-":
                    return math.unaryMinus();
                case "~":
                    return math.bitwiseComplement();
                default:
                    return null;
            }
        } catch (UnsupportedOperationException e) {
            // ~ on a floating-point operand
            return null;
        }
    }

    /**
     * Returns the value of a binary operation on two constants.
     *
     * @param op the operator token
     * @param left the left operand
     * @param right the right operand
     * @return the result, or null if it is not a constant
     */
    static @Nullable Object binary(String op, Object left, Object right) {
        if (op.equals("+") && (left instanceof String || right instanceof String)) {
            // String conversion (JLS 5.1.11), which String.valueOf performs for every constant
            // type, including a char, which is appended as a character.
            return String.valueOf(left) + right;
        }
        if (left instanceof Boolean && right instanceof Boolean) {
            boolean l = (Boolean) left;
            boolean r = (Boolean) right;
            switch (op) {
                case "&":
                case "&&":
                    return l && r;
                case "|":
                case "||":
                    return l || r;
                case "^":
                case "!=":
                    return l != r;
                case "==":
                    return l == r;
                default:
                    return null;
            }
        }
        Number l = numeric(left);
        Number r = numeric(right);
        NumberMath<?> math = l == null || r == null ? null : NumberMath.getNumberMath(l);
        if (math == null) {
            return null;
        }
        try {
            switch (op) {
                case "+":
                    return math.plus(r);
                case "-":
                    return math.minus(r);
                case "*":
                    return math.times(r);
                case "/":
                    return math.divide(r);
                case "%":
                    return math.remainder(r);
                case "<<":
                    return math.shiftLeft(r);
                case ">>":
                    return math.signedShiftRight(r);
                case ">>>":
                    return math.unsignedShiftRight(r);
                case "&":
                    return math.bitwiseAnd(r);
                case "|":
                    return math.bitwiseOr(r);
                case "^":
                    return math.bitwiseXor(r);
                case "==":
                    return math.equalTo(r);
                case "!=":
                    return math.notEqualTo(r);
                case "<":
                    return math.lessThan(r);
                case "<=":
                    return math.lessThanEq(r);
                case ">":
                    return math.greaterThan(r);
                case ">=":
                    return math.greaterThanEq(r);
                default:
                    return null;
            }
        } catch (UnsupportedOperationException e) {
            // A shift or bitwise operation on a floating-point operand.
            return null;
        }
    }

    /**
     * Returns the value of a conditional expression {@code condition ? ifTrue : ifFalse}. Two
     * numeric operands of different types are promoted as by binary numeric promotion; the special
     * rules of JLS 15.25 for a constant {@code int} operand whose value fits a narrower type are
     * not applied, which can only change the type of an {@code int} result that is then converted
     * to the annotation element's type anyway.
     *
     * @param condition the condition
     * @param ifTrue the second operand
     * @param ifFalse the third operand
     * @return the result, or null if it is not a constant
     */
    static @Nullable Object conditional(Object condition, Object ifTrue, Object ifFalse) {
        if (!(condition instanceof Boolean)) {
            return null;
        }
        Object result = (Boolean) condition ? ifTrue : ifFalse;
        if (ifTrue.getClass() == ifFalse.getClass()) {
            return result;
        }
        Number t = numeric(ifTrue);
        Number f = numeric(ifFalse);
        if (t == null || f == null) {
            return null;
        }
        String promoted;
        if (t instanceof Double || f instanceof Double) {
            promoted = "double";
        } else if (t instanceof Float || f instanceof Float) {
            promoted = "float";
        } else if (t instanceof Long || f instanceof Long) {
            promoted = "long";
        } else {
            promoted = "int";
        }
        return cast(promoted, result);
    }

    /**
     * Returns the value of a cast of a constant to a primitive type or to {@code String}.
     *
     * @param type the type: a primitive type's name, {@code "String"}, or {@code
     *     "java.lang.String"}
     * @param operand the operand
     * @return the result, or null if it is not a constant
     */
    static @Nullable Object cast(String type, Object operand) {
        if (type.equals("String") || type.equals("java.lang.String")) {
            return operand instanceof String ? operand : null;
        }
        if (type.equals("boolean")) {
            return operand instanceof Boolean ? operand : null;
        }
        Number n = numeric(operand);
        if (n == null) {
            return null;
        }
        switch (type) {
            case "byte":
                return n.byteValue();
            case "short":
                return n.shortValue();
            case "char":
                return (char) n.intValue();
            case "int":
                return n.intValue();
            case "long":
                return n.longValue();
            case "float":
                return n.floatValue();
            case "double":
                return n.doubleValue();
            default:
                return null;
        }
    }

    /**
     * Returns the field that a simple name in an annotation value refers to, looking it up the way
     * Java scopes a simple name, as far as a stub file allows: first among the fields of {@code
     * enclosingClass} and of each class that lexically encloses it (each including inherited
     * fields), then among the static fields of the candidate classes, in order. The caller lists as
     * candidates the classes of the stub file's single-static-imports of that name, followed by the
     * classes of its static-import-on-demand declarations ({@code import static C.*;}), each in the
     * order of the imports.
     *
     * @param name the simple name
     * @param enclosingClass the class whose member, or which itself, the annotation is on, or null
     *     for an annotation on a package or module
     * @param candidateClasses the fully-qualified names of the classes that the stub file's static
     *     imports can import {@code name} from
     * @param elements the element utilities
     * @return the field, or null if there is none
     */
    static @Nullable VariableElement findConstant(
            String name,
            @Nullable TypeElement enclosingClass,
            List<String> candidateClasses,
            Elements elements) {
        for (Element scope = enclosingClass;
                scope != null && (scope.getKind().isClass() || scope.getKind().isInterface());
                scope = scope.getEnclosingElement()) {
            VariableElement field = findField((TypeElement) scope, name, elements, false);
            if (field != null) {
                return field;
            }
        }
        for (String candidate : candidateClasses) {
            TypeElement type = getTypeElement(candidate, elements);
            VariableElement field = type == null ? null : findField(type, name, elements, true);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    /**
     * Returns the type element with the given canonical name, or null if there is none.
     *
     * @param canonicalName a canonical name
     * @param elements the element utilities
     * @return the type element, or null
     */
    @SuppressWarnings("signature:argument.type.incompatible") // a stub file's import names a class
    private static @Nullable TypeElement getTypeElement(String canonicalName, Elements elements) {
        return elements.getTypeElement(canonicalName);
    }

    /**
     * Returns the field or enum constant named {@code name} of {@code type}, including inherited
     * ones, or null if there is none.
     *
     * @param type a class
     * @param name a simple name
     * @param elements the element utilities
     * @param staticOnly if true, only a static field counts, as for a static import
     * @return the field, or null
     */
    static @Nullable VariableElement findField(
            TypeElement type, String name, Elements elements, boolean staticOnly) {
        for (VariableElement field : ElementUtils.getAllFieldsIn(type, elements)) {
            if ((field.getKind() == ElementKind.FIELD
                            || field.getKind() == ElementKind.ENUM_CONSTANT)
                    && field.getSimpleName().contentEquals(name)
                    && (!staticOnly || field.getModifiers().contains(Modifier.STATIC))) {
                return field;
            }
        }
        return null;
    }
}
