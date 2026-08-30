package org.checkerframework.common.value;

import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionConverter;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.ValueLiteral;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

/**
 * Optimize the given JavaExpression. If the supplied factory is a {@code
 * ValueAnnotatedTypeFactory}, this implementation replaces any expression that the factory has an
 * exact value for, and does a small (not exhaustive) amount of constant-folding as well. If the
 * factory is some other factory, less optimization occurs.
 */
public class JavaExpressionOptimizer extends JavaExpressionConverter {

    /**
     * Annotated type factory. If it is a {@code ValueAnnotatedTypeFactory}, then more optimizations
     * are possible.
     */
    private final AnnotatedTypeFactory atypeFactory;

    /**
     * Creates a JavaExpressionOptimizer.
     *
     * @param atypeFactory an annotated type factory
     */
    public JavaExpressionOptimizer(AnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
    }

    @Override
    protected JavaExpression visitFieldAccess(FieldAccess fieldAccessExpr, Void unused) {
        // Replace references to compile-time constant fields by the constant itself.
        if (fieldAccessExpr.isFinal()) {
            Object constant = fieldAccessExpr.getField().getConstantValue();
            if (constant != null && !(constant instanceof String)) {
                return new ValueLiteral(fieldAccessExpr.getType(), constant);
            }
        }
        return super.visitFieldAccess(fieldAccessExpr, unused);
    }

    @Override
    protected JavaExpression visitLocalVariable(LocalVariable localVarExpr, Void unused) {
        if (atypeFactory instanceof ValueAnnotatedTypeFactory) {
            Element element = localVarExpr.getElement();
            Long exactValue =
                    ValueCheckerUtils.getExactValue(
                            element, (ValueAnnotatedTypeFactory) atypeFactory);
            if (exactValue != null) {
                // The exact value is stored as a Long.  Narrow it back to the variable's
                // declared primitive type before constructing the ValueLiteral; otherwise
                // a 'long' variable would be replaced by a ValueLiteral whose Java type is
                // long but whose runtime value is an Integer, which silently truncates
                // values outside the int range and confuses consumers such as
                // ValueLiteral.negate() that branch on the value's runtime class.
                Object literalValue;
                switch (localVarExpr.getType().getKind()) {
                    case BYTE:
                        literalValue = exactValue.byteValue();
                        break;
                    case SHORT:
                        literalValue = exactValue.shortValue();
                        break;
                    case INT:
                        literalValue = exactValue.intValue();
                        break;
                    case LONG:
                        literalValue = exactValue;
                        break;
                    case CHAR:
                        literalValue = (char) exactValue.longValue();
                        break;
                    default:
                        // Don't optimize for boxed types, references, etc.
                        return super.visitLocalVariable(localVarExpr, unused);
                }
                return new ValueLiteral(localVarExpr.getType(), literalValue);
            }
        }
        return super.visitLocalVariable(localVarExpr, unused);
    }

    @Override
    protected JavaExpression visitMethodCall(MethodCall methodCallExpr, Void unused) {
        JavaExpression optReceiver = convert(methodCallExpr.getReceiver());
        List<JavaExpression> optArguments = convert(methodCallExpr.getArguments());
        // Length of string literal: convert it to an integer literal.
        if (methodCallExpr.getElement().getSimpleName().contentEquals("length")
                && optReceiver instanceof ValueLiteral) {
            Object value = ((ValueLiteral) optReceiver).getValue();
            if (value instanceof String) {
                return new ValueLiteral(
                        atypeFactory.types.getPrimitiveType(TypeKind.INT),
                        ((String) value).length());
            }
        }
        return new MethodCall(
                methodCallExpr.getType(), methodCallExpr.getElement(), optReceiver, optArguments);
    }
}
