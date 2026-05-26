package org.checkerframework.framework.testchecker.commonassignment;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.commonassignment.quals.CommonInvalid;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;

import javax.lang.model.element.AnnotationMirror;

/**
 * Visitor for {@link CommonAssignmentChecker}.
 *
 * <p>This visitor exercises two behaviors of the framework that interact:
 *
 * <ol>
 *   <li>A custom {@link BaseTypeValidator} that flags types annotated with {@code @CommonInvalid}
 *       as invalid.
 *   <li>An override of {@code commonAssignmentCheck(Tree, ExpressionTree, ...)} that issues a
 *       <em>secondary</em> warning, but only if the parent's result was {@code true}. This mimics
 *       what real-world subclasses (for example {@code LowerBoundVisitor}) do: they compose their
 *       own check with the parent's result via {@code &&}.
 * </ol>
 *
 * <p>If the parent {@code commonAssignmentCheck(Tree, ...)} were to return {@code true} after a
 * failed {@code validateType} call (the bug fixed by PR #736), this override would emit the
 * secondary warning even though the LHS type was already known to be invalid. With the fix, the
 * parent returns {@code false} and the secondary warning is correctly suppressed.
 */
public class CommonAssignmentVisitor extends BaseTypeVisitor<CommonAssignmentAnnotatedTypeFactory> {

    /**
     * Creates a {@link CommonAssignmentVisitor}.
     *
     * @param checker the associated checker
     */
    public CommonAssignmentVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new CommonAssignmentTypeValidator(checker, this, atypeFactory);
    }

    @Override
    protected boolean commonAssignmentCheck(
            Tree varTree,
            ExpressionTree valueExpTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        boolean superResult =
                super.commonAssignmentCheck(varTree, valueExpTree, errorKey, extraArgs);
        // Emit a secondary warning only if the parent reports success. If the parent's
        // post-validation contract is correct (return false when validateType failed), this
        // warning will not be issued for assignments whose LHS has an invalid type.
        if (superResult) {
            checker.reportWarning(varTree, "commonassignment.parent.succeeded");
        }
        return superResult;
    }

    /**
     * A type validator that reports types containing {@code @CommonInvalid} as invalid via {@link
     * #reportInvalidType}, which sets {@code isValid = false} and causes {@link
     * BaseTypeVisitor#validateType} to return {@code false}.
     */
    private final class CommonAssignmentTypeValidator extends BaseTypeValidator {

        /**
         * Creates a {@link CommonAssignmentTypeValidator}.
         *
         * @param checker the associated checker
         * @param visitor the associated visitor
         * @param atypeFactory the associated type factory
         */
        CommonAssignmentTypeValidator(
                BaseTypeChecker checker,
                BaseTypeVisitor<?> visitor,
                AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree p) {
            AnnotationMirror invalidAnno =
                    AnnotationBuilder.fromClass(elements, CommonInvalid.class);
            if (AnnotatedTypes.containsModifier(type, invalidAnno)) {
                // reportInvalidType issues "type.invalid" and sets isValid = false.
                reportInvalidType(type, p);
            }
            return super.visitDeclared(type, p);
        }
    }
}
