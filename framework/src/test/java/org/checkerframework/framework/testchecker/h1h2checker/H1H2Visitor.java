package org.checkerframework.framework.testchecker.h1h2checker;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Invalid;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;

import javax.lang.model.element.AnnotationMirror;

public class H1H2Visitor extends BaseTypeVisitor<H1H2AnnotatedTypeFactory> {

    /** Variable name used by {@code CommonAssignmentReturn} to exercise invalid LHS validation. */
    private static final String COMMON_ASSIGNMENT_INVALID_LHS = "commonAssignmentInvalid";

    public H1H2Visitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new H1H2TypeValidator(checker, this, atypeFactory);
    }

    @Override
    protected boolean commonAssignmentCheck(
            Tree varTree,
            ExpressionTree valueExpTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        AnnotationMirror h1Invalid = AnnotationBuilder.fromClass(elements, H1Invalid.class);
        boolean invalidLhs =
                AnnotatedTypes.containsModifier(
                        atypeFactory.getAnnotatedTypeLhs(varTree), h1Invalid);
        boolean superResult =
                super.commonAssignmentCheck(varTree, valueExpTree, errorKey, extraArgs);
        // This is a regression-test sentinel for BaseTypeVisitor.commonAssignmentCheck(Tree, ...).
        // If the parent returns true after validateType rejects an invalid LHS, this warning is
        // unexpected and the test fails.
        if (superResult && invalidLhs) {
            checker.reportWarning(varTree, "h1h2checker.commonassignment.parent.succeeded");
        }
        return superResult;
    }

    /** Returns true if {@code tree} is the invalid LHS used by {@code CommonAssignmentReturn}. */
    private boolean isCommonAssignmentInvalidLhs(Tree tree) {
        switch (tree.getKind()) {
            case VARIABLE:
                return ((VariableTree) tree).getName().contentEquals(COMMON_ASSIGNMENT_INVALID_LHS);
            case IDENTIFIER:
                return ((IdentifierTree) tree)
                        .getName()
                        .contentEquals(COMMON_ASSIGNMENT_INVALID_LHS);
            default:
                return false;
        }
    }

    /** Type validator that handles the invalid-type qualifiers in the H1 hierarchy. */
    private final class H1H2TypeValidator extends BaseTypeValidator {

        /**
         * Creates an {@link H1H2TypeValidator}.
         *
         * @param checker the associated checker
         * @param visitor the associated visitor
         * @param atypeFactory the associated type factory
         */
        H1H2TypeValidator(
                BaseTypeChecker checker,
                BaseTypeVisitor<?> visitor,
                AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree p) {
            AnnotationMirror h1Invalid = AnnotationBuilder.fromClass(elements, H1Invalid.class);
            if (AnnotatedTypes.containsModifier(type, h1Invalid)) {
                if (isCommonAssignmentInvalidLhs(p)) {
                    reportInvalidType(type, p);
                    return super.visitDeclared(type, p);
                }
                checker.reportError(
                        p,
                        // An error specific to this type system, with no corresponding text
                        // in a messages.properties file; this checker is just for testing.
                        "h1h2checker.h1invalid.forbidden",
                        type.getAnnotations(),
                        type.toString());
            }
            return super.visitDeclared(type, p);
        }
    }
}
