package org.checkerframework.checker.signedness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.interning.InterningVisitor;
import org.checkerframework.checker.interning.qual.EqualsMethod;
import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The SignednessVisitor enforces the Signedness Checker rules. These rules are described in the
 * Checker Framework Manual.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
public class SignednessVisitor extends BaseTypeVisitor<SignednessAnnotatedTypeFactory> {

    public SignednessVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Determines if an annotated type is annotated as {@link Unsigned} or {@link PolySigned}
     *
     * @param type the annotated type to be checked
     * @return true if the annotated type is annotated as {@link Unsigned} or {@link PolySigned}
     */
    private boolean hasUnsignedAnnotation(AnnotatedTypeMirror type) {
        return type.hasAnnotation(Unsigned.class) || type.hasAnnotation(PolySigned.class);
    }

    /**
     * Determines if an annotated type is annotated as {@link Signed} or {@link PolySigned}
     *
     * @param type the annotated type to be checked
     * @return true if the annotated type is annotated as {@link Signed} or {@link PolySigned}
     */
    private boolean hasSignedAnnotation(AnnotatedTypeMirror type) {
        return type.hasAnnotation(Signed.class) || type.hasAnnotation(PolySigned.class);
    }

    /**
     * Enforces the following rules on binary operations involving Unsigned and Signed types:
     *
     * <ul>
     *   <li>Do not allow any Unsigned types or PolySigned types in {@literal {/, %}} operations.
     *   <li>Do not allow signed right shift {@literal {>>}} on an Unsigned type or a PolySigned
     *       type.
     *   <li>Do not allow unsigned right shift {@literal {>>>}} on a Signed type or a PolySigned
     *       type.
     *   <li>Allow any left shift {@literal {<<}}.
     *   <li>Do not allow non-equality comparisons {@literal {<, <=, >, >=}} on Unsigned types or
     *       PolySigned types.
     *   <li>Do not allow the mixing of Signed and Unsigned types.
     * </ul>
     */
    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        // Used in diagnostic messages.
        ExpressionTree leftOp = node.getLeftOperand();
        ExpressionTree rightOp = node.getRightOperand();

        Pair<AnnotatedTypeMirror, AnnotatedTypeMirror> argTypes =
                atypeFactory.binaryTreeArgTypes(node);
        AnnotatedTypeMirror leftOpType = argTypes.first;
        AnnotatedTypeMirror rightOpType = argTypes.second;

        Tree.Kind kind = node.getKind();

        switch (kind) {
            case DIVIDE:
            case REMAINDER:
                if (hasUnsignedAnnotation(leftOpType)) {
                    checker.reportError(
                            leftOp, "operation.unsignedlhs", kind, leftOpType, rightOpType);
                } else if (hasUnsignedAnnotation(rightOpType)) {
                    checker.reportError(
                            rightOp, "operation.unsignedrhs", kind, leftOpType, rightOpType);
                }
                break;

            case RIGHT_SHIFT:
                if (hasUnsignedAnnotation(leftOpType)
                        && !atypeFactory.isMaskedShiftEitherSignedness(node, getCurrentPath())
                        && !atypeFactory.isCastedShiftEitherSignedness(node, getCurrentPath())) {
                    checker.reportError(leftOp, "shift.signed", kind, leftOpType, rightOpType);
                }
                break;

            case UNSIGNED_RIGHT_SHIFT:
                if (hasSignedAnnotation(leftOpType)
                        && !atypeFactory.isMaskedShiftEitherSignedness(node, getCurrentPath())
                        && !atypeFactory.isCastedShiftEitherSignedness(node, getCurrentPath())) {
                    checker.reportError(leftOp, "shift.unsigned", kind, leftOpType, rightOpType);
                }
                break;

            case LEFT_SHIFT:
                break;

            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
            case LESS_THAN:
            case LESS_THAN_EQUAL:
                if (hasUnsignedAnnotation(leftOpType)) {
                    checker.reportError(leftOp, "comparison.unsignedlhs", leftOpType, rightOpType);
                } else if (hasUnsignedAnnotation(rightOpType)) {
                    checker.reportError(rightOp, "comparison.unsignedrhs", leftOpType, rightOpType);
                }
                break;

            case EQUAL_TO:
            case NOT_EQUAL_TO:
                if (!atypeFactory.maybeIntegral(leftOpType)
                        || !atypeFactory.maybeIntegral(rightOpType)) {
                    break;
                }
                if (leftOpType.hasAnnotation(Unsigned.class)
                        && rightOpType.hasAnnotation(Signed.class)) {
                    checker.reportError(
                            node, "comparison.mixed.unsignedlhs", leftOpType, rightOpType);
                } else if (leftOpType.hasAnnotation(Signed.class)
                        && rightOpType.hasAnnotation(Unsigned.class)) {
                    checker.reportError(
                            node, "comparison.mixed.unsignedrhs", leftOpType, rightOpType);
                }
                break;

            case PLUS:
                if (TreeUtils.isStringConcatenation(node)) {
                    AnnotationMirror leftAnno =
                            leftOpType.getEffectiveAnnotations().iterator().next();
                    AnnotationMirror rightAnno =
                            rightOpType.getEffectiveAnnotations().iterator().next();

                    // Note that leftOpType.getUnderlyingType() and rightOpType.getUnderlyingType()
                    // are always java.lang.String. Please refer to binaryTreeArgTypes for more
                    // details.
                    // Here, the original types of operands can be something different from string.
                    // For example, "123" + obj is a string concatenation in which the original type
                    // of the right operand is java.lang.Object.
                    TypeMirror leftOpOriginalType = TreeUtils.typeOf(leftOp);
                    TypeMirror rightOpOriginalType = TreeUtils.typeOf(rightOp);

                    if (!TypesUtils.isCharType(leftOpOriginalType)
                            && !atypeFactory
                                    .getQualifierHierarchy()
                                    .isSubtype(leftAnno, atypeFactory.SIGNED)) {
                        checker.reportError(leftOp, "unsigned.concat");
                    } else if (!TypesUtils.isCharType(rightOpOriginalType)
                            && !atypeFactory
                                    .getQualifierHierarchy()
                                    .isSubtype(rightAnno, atypeFactory.SIGNED)) {
                        checker.reportError(rightOp, "unsigned.concat");
                    }
                    break;
                }
                // Other plus binary trees should be handled in the default case.
                // fall through
            default:
                if (leftOpType.hasAnnotation(Unsigned.class)
                        && rightOpType.hasAnnotation(Signed.class)) {
                    checker.reportError(
                            node, "operation.mixed.unsignedlhs", kind, leftOpType, rightOpType);
                } else if (leftOpType.hasAnnotation(Signed.class)
                        && rightOpType.hasAnnotation(Unsigned.class)) {
                    checker.reportError(
                            node, "operation.mixed.unsignedrhs", kind, leftOpType, rightOpType);
                }
                break;
        }
        return super.visitBinary(node, p);
    }

    // Ensure that method annotations are not written on methods they don't apply to.
    // Copied from InterningVisitor
    @Override
    public Void visitMethod(MethodTree node, Void p) {
        ExecutableElement methElt = TreeUtils.elementFromDeclaration(node);
        boolean hasEqualsMethodAnno =
                atypeFactory.getDeclAnnotation(methElt, EqualsMethod.class) != null;
        int params = methElt.getParameters().size();
        if (hasEqualsMethodAnno && !(params == 1 || params == 2)) {
            checker.reportError(
                    node, "invalid.method.annotation", "@EqualsMethod", "1 or 2", methElt, params);
        }

        return super.visitMethod(node, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        ExecutableElement methElt = TreeUtils.elementFromUse(node);
        boolean hasEqualsMethodAnno =
                atypeFactory.getDeclAnnotation(methElt, EqualsMethod.class) != null;
        if (hasEqualsMethodAnno || InterningVisitor.isInvocationOfEquals(node)) {
            int params = methElt.getParameters().size();
            if (!(params == 1 || params == 2)) {
                checker.reportError(
                        node,
                        "invalid.method.annotation",
                        "@EqualsMethod",
                        "1 or 2",
                        methElt,
                        params);
            } else {
                AnnotatedTypeMirror leftOpType;
                AnnotatedTypeMirror rightOpType;
                if (params == 1) {
                    leftOpType = atypeFactory.getReceiverType(node);
                    rightOpType = atypeFactory.getAnnotatedType(node.getArguments().get(0));
                } else if (params == 2) {
                    leftOpType = atypeFactory.getAnnotatedType(node.getArguments().get(0));
                    rightOpType = atypeFactory.getAnnotatedType(node.getArguments().get(1));
                } else {
                    throw new BugInCF("Checked that params is 1 or 2");
                }
                if (!atypeFactory.maybeIntegral(leftOpType)
                        || !atypeFactory.maybeIntegral(rightOpType)) {
                    // nothing to do
                } else if (leftOpType.hasAnnotation(Unsigned.class)
                        && rightOpType.hasAnnotation(Signed.class)) {
                    checker.reportError(
                            node, "comparison.mixed.unsignedlhs", leftOpType, rightOpType);
                } else if (leftOpType.hasAnnotation(Signed.class)
                        && rightOpType.hasAnnotation(Unsigned.class)) {
                    checker.reportError(
                            node, "comparison.mixed.unsignedrhs", leftOpType, rightOpType);
                }
            }
            // Don't check against the annotated method declaration (which super would do).
            return null;
        }

        return super.visitMethodInvocation(node, p);
    }

    /**
     * Returns a string representation of {@code kind}, with trailing _ASSIGNMENT stripped off if
     * any.
     *
     * @param kind a tree kind
     * @return a string representation of {@code kind}, with trailing _ASSIGNMENT stripped off if
     *     any
     */
    private String kindWithoutAssignment(Tree.Kind kind) {
        String result = kind.toString();
        if (result.endsWith("_ASSIGNMENT")) {
            return result.substring(0, result.length() - "_ASSIGNMENT".length());
        } else {
            return result;
        }
    }

    /**
     * Enforces the following rules on compound assignments involving Unsigned and Signed types:
     *
     * <ul>
     *   <li>Do not allow any Unsigned types or PolySigned types in {@literal {/=, %=}} assignments.
     *   <li>Do not allow signed right shift {@literal {>>=}} to assign to an Unsigned type or a
     *       PolySigned type.
     *   <li>Do not allow unsigned right shift {@literal {>>>=}} to assign to a Signed type or a
     *       PolySigned type.
     *   <li>Allow any left shift {@literal {<<=}} assignment.
     *   <li>Do not allow mixing of Signed and Unsigned types.
     * </ul>
     */
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {

        ExpressionTree var = node.getVariable();
        ExpressionTree expr = node.getExpression();

        Pair<AnnotatedTypeMirror, AnnotatedTypeMirror> argTypes =
                atypeFactory.compoundAssignmentTreeArgTypes(node);
        AnnotatedTypeMirror varType = argTypes.first;
        AnnotatedTypeMirror exprType = argTypes.second;

        Tree.Kind kind = node.getKind();

        switch (kind) {
            case DIVIDE_ASSIGNMENT:
            case REMAINDER_ASSIGNMENT:
                if (hasUnsignedAnnotation(varType)) {
                    checker.reportError(
                            var,
                            "compound.assignment.unsigned.variable",
                            kindWithoutAssignment(kind),
                            varType,
                            exprType);
                } else if (hasUnsignedAnnotation(exprType)) {
                    checker.reportError(
                            expr,
                            "compound.assignment.unsigned.expression",
                            kindWithoutAssignment(kind),
                            varType,
                            exprType);
                }
                break;

            case RIGHT_SHIFT_ASSIGNMENT:
                if (hasUnsignedAnnotation(varType)) {
                    checker.reportError(
                            var,
                            "compound.assignment.shift.signed",
                            kindWithoutAssignment(kind),
                            varType,
                            exprType);
                }
                break;

            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                if (hasSignedAnnotation(varType)) {
                    checker.reportError(
                            var,
                            "compound.assignment.shift.unsigned",
                            kindWithoutAssignment(kind),
                            varType,
                            exprType);
                }
                break;

            case LEFT_SHIFT_ASSIGNMENT:
                break;

            case PLUS_ASSIGNMENT:
                if (TreeUtils.isStringCompoundConcatenation(node)) {
                    // Note that exprType.getUnderlyingType() is always java.lang.String.
                    // Please refer to compoundAssignmentTreeArgTypes for more details.
                    if (!TypesUtils.isCharType(TreeUtils.typeOf(expr))) {
                        AnnotationMirror anno =
                                exprType.getEffectiveAnnotations().iterator().next();
                        if (!atypeFactory
                                .getQualifierHierarchy()
                                .isSubtype(anno, atypeFactory.SIGNED)) {
                            checker.reportError(node.getExpression(), "unsigned.concat");
                        }
                    }
                    break;
                }
                // Other plus binary trees should be handled in the default case.
                // fall through
            default:
                if (varType.hasAnnotation(Unsigned.class) && exprType.hasAnnotation(Signed.class)) {
                    checker.reportError(
                            expr,
                            "compound.assignment.mixed.unsigned.variable",
                            kindWithoutAssignment(kind),
                            varType,
                            exprType);
                } else if (varType.hasAnnotation(Signed.class)
                        && exprType.hasAnnotation(Unsigned.class)) {
                    checker.reportError(
                            expr,
                            "compound.assignment.mixed.unsigned.expression",
                            kindWithoutAssignment(kind),
                            varType,
                            exprType);
                }
                break;
        }
        return super.visitCompoundAssignment(node, p);
    }

    @Override
    protected boolean isTypeCastSafe(AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType) {
        if (!atypeFactory.maybeIntegral(castType)) {
            // If the cast is not a number or a char, then it is legal.
            return true;
        }
        return super.isTypeCastSafe(castType, exprType);
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        return Collections.singleton(atypeFactory.SIGNED);
    }

    @Override
    protected void checkConstructorResult(
            AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {}

    @Override
    protected boolean shouldWarnAboutIrrelevantJavaTypes() {
        return true;
    }
}
