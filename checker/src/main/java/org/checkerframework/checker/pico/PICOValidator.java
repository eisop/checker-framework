package org.checkerframework.checker.pico;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/**
 * Enforces well-formed PICO immutability, receiver-dependent mutability, and assignability
 * annotations.
 */
public class PICOValidator extends BaseTypeValidator {
    /** The type factory for the PICO checker. */
    private final PICONoInitAnnotatedTypeFactory picoTypeFactory;

    /**
     * Create a new PICOValidator.
     *
     * @param checker the checker
     * @param visitor the visitor
     * @param atypeFactory the type factory
     */
    public PICOValidator(
            BaseTypeChecker checker,
            BaseTypeVisitor<?> visitor,
            AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
        this.picoTypeFactory = (PICONoInitAnnotatedTypeFactory) atypeFactory;
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
        checkClassBound(type, tree);
        checkStaticReceiverDependentMutableError(type, tree);
        checkImplicitlyImmutableTypeError(type, tree);
        checkFieldAssignabilityDeclaration(tree);

        return super.visitDeclared(type, tree);
    }

    @Override
    protected boolean shouldCheckTopLevelDeclaredOrPrimitiveType(
            AnnotatedTypeMirror type, Tree tree) {
        if (isReceiverDependentUseOfMutableFieldInMutableClass(type, tree)) {
            return false;
        }
        if (type.getKind() != TypeKind.DECLARED && !type.getKind().isPrimitive()) {
            return true;
        }
        // Unlike BaseTypeValidator, PICO still checks local variable declarations.
        return !TreeUtils.isExpressionTree(tree) || TreeUtils.isTypeTree(tree);
    }

    /**
     * Allows {@code @ReceiverDependentMutable} on fields whose declared type is mutable when the
     * enclosing class is also mutable. In that case, the field type is receiver-dependent but every
     * possible receiver-bound result is within the declared mutable bound.
     *
     * @param type field type
     * @param tree tree whose type is being validated
     * @return true if this type use should skip the top-level declaration-bound check
     */
    private boolean isReceiverDependentUseOfMutableFieldInMutableClass(
            AnnotatedTypeMirror type, Tree tree) {
        if (!type.hasAnnotation(picoTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
            return false;
        }

        VariableElement field = getFieldElement(tree);
        if (field == null) {
            return false;
        }

        TypeElement enclosingClass = ElementUtils.enclosingTypeElement(field);
        if (enclosingClass == null) {
            return false;
        }

        @Immutable AnnotationMirrorSet enclosingBound =
                atypeFactory.getTypeDeclarationBounds(enclosingClass.asType());
        if (!AnnotationUtils.containsSameByName(enclosingBound, picoTypeFactory.MUTABLE)) {
            return false;
        }

        @Immutable AnnotationMirrorSet declaredBound =
                atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType());
        return AnnotationUtils.containsSameByName(declaredBound, picoTypeFactory.MUTABLE);
    }

    @Override
    public Void visitArray(AnnotatedArrayType type, Tree tree) {
        checkStaticReceiverDependentMutableError(type, tree);
        // Array can not be implicitly immutable
        return super.visitArray(type, tree);
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        checkImplicitlyImmutableTypeError(type, tree);
        checkFieldAssignabilityDeclaration(tree);
        return super.visitPrimitive(type, tree);
    }

    /**
     * Reject receiver-dependent mutable type uses in static contexts. A receiver-dependent class
     * declaration may itself be static, but members and other type uses in static contexts have no
     * receiver to depend on.
     *
     * @param type the type to check
     * @param tree the tree to check
     */
    private void checkStaticReceiverDependentMutableError(AnnotatedTypeMirror type, Tree tree) {
        if (!type.isDeclaration()
                && TreePathUtil.isTreeInStaticScope(visitor.getCurrentPath())
                && type.hasAnnotation(picoTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
            reportValidityResult("static.receiverdependentmutable.forbidden", type, tree);
        }
    }

    /**
     * Check that implicitly immutable type has immutable or bottom type. Dataflow might refine
     * immutable type to {@code @Bottom} (see RefineFromNull.java), so we accept @Bottom as a valid
     * qualifier for implicitly immutable types.
     *
     * @param type the type to check
     * @param tree the tree to check
     */
    private void checkImplicitlyImmutableTypeError(AnnotatedTypeMirror type, Tree tree) {
        if (PICOTypeUtil.isImplicitlyImmutableType(type)
                && !type.hasAnnotation(picoTypeFactory.IMMUTABLE)
                && !type.hasAnnotation(picoTypeFactory.BOTTOM)) {
            reportInvalidAnnotationsOnUse(type, tree);
        }
    }

    /**
     * Checks that class declarations use one of the valid PICO class bounds.
     *
     * @param type the class declaration type
     * @param tree the class declaration tree
     */
    private void checkClassBound(AnnotatedDeclaredType type, Tree tree) {
        if (!type.isDeclaration()
                || !(tree instanceof ClassTree)
                || TypesUtils.isAnonymous(TreeUtils.typeOf((ClassTree) tree))) {
            return;
        }

        if (!PICOTypeUtil.isValidClassBound(type, picoTypeFactory)) {
            checker.reportError(tree, "class.bound.invalid", type);
            isValid = false;
        }
    }

    /**
     * Ensures the well-formedness in terms of assignability on a field. This covers both instance
     * fields and static fields.
     *
     * @param tree the tree to check
     */
    private void checkFieldAssignabilityDeclaration(Tree tree) {
        VariableElement field = getFieldElement(tree);
        if (field == null) {
            return;
        }
        if (!PICOTypeUtil.hasOneAndOnlyOneAssignabilityQualifier(field, atypeFactory)) {
            checker.reportError(field, "assignability.declaration.invalid", field);
            isValid = false;
        }
    }

    /**
     * Returns the field declared by {@code tree}, or null if {@code tree} does not declare a field.
     *
     * @param tree the tree to inspect
     * @return the declared field element, or null
     */
    private VariableElement getFieldElement(Tree tree) {
        if (!(tree instanceof VariableTree)) {
            return null;
        }
        VariableElement variableElement = TreeUtils.elementFromDeclaration((VariableTree) tree);
        return variableElement.getKind() == ElementKind.FIELD ? variableElement : null;
    }
}
