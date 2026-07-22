package org.checkerframework.checker.mutability;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

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

/** Checks mutability-specific validity rules that are not represented by ordinary subtyping. */
public class MutabilityValidator extends BaseTypeValidator {
    /** The type factory for the mutability no-initialization checker. */
    private final MutabilityNoInitAnnotatedTypeFactory mutabilityTypeFactory;

    /**
     * Create a MutabilityValidator.
     *
     * @param checker the checker
     * @param visitor the visitor
     * @param atypeFactory the type factory
     */
    public MutabilityValidator(
            BaseTypeChecker checker,
            BaseTypeVisitor<?> visitor,
            AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
        this.mutabilityTypeFactory = (MutabilityNoInitAnnotatedTypeFactory) atypeFactory;
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
        // Mutability also validates local variable declarations.
        return !TreeUtils.isExpressionTree(tree) || TreeUtils.isTypeTree(tree);
    }

    /**
     * Returns true if the top-level declaration-bound check should be skipped for this type.
     *
     * <p>Mutability permits {@code @ReceiverDependentMutable} on a field with a mutable declared
     * bound inside a mutable class. Every receiver-dependent result is still within the declared
     * mutable bound, so the regular declaration-bound check would be too strict.
     *
     * @param type field type
     * @param tree tree whose type is being validated
     * @return true if this type use should skip the top-level declaration-bound check
     */
    private boolean isReceiverDependentUseOfMutableFieldInMutableClass(
            AnnotatedTypeMirror type, Tree tree) {
        if (!type.hasAnnotation(mutabilityTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
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

        AnnotationMirrorSet enclosingBound =
                atypeFactory.getTypeDeclarationBounds(enclosingClass.asType());
        if (!AnnotationUtils.containsSameByName(enclosingBound, mutabilityTypeFactory.MUTABLE)) {
            return false;
        }

        AnnotationMirrorSet declaredBound =
                atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType());
        return AnnotationUtils.containsSameByName(declaredBound, mutabilityTypeFactory.MUTABLE);
    }

    @Override
    public Void visitArray(AnnotatedArrayType type, Tree tree) {
        checkStaticReceiverDependentMutableError(type, tree);
        // Array declaration bounds are receiver-dependent in the mutability checker, so
        // implicit-immutable validation
        // does not apply.
        return super.visitArray(type, tree);
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        checkImplicitlyImmutableTypeError(type, tree);
        checkFieldAssignabilityDeclaration(tree);
        return super.visitPrimitive(type, tree);
    }

    /**
     * Reports an error for receiver-dependent mutable type uses in static contexts.
     *
     * <p>A receiver-dependent class declaration may itself be static, but members and other type
     * uses in static contexts have no receiver to depend on.
     *
     * @param type the type to check
     * @param tree the tree to check
     */
    private void checkStaticReceiverDependentMutableError(AnnotatedTypeMirror type, Tree tree) {
        if (!type.isDeclaration()
                && TreePathUtil.isTreeInStaticScope(visitor.getCurrentPath())
                && type.hasAnnotation(mutabilityTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
            reportValidityResult("static.receiverdependentmutable.forbidden", type, tree);
        }
    }

    /**
     * Reports an error if an implicitly immutable type is used with a non-immutable qualifier.
     *
     * <p>Dataflow may refine an immutable type to {@code @Bottom}, as in {@code RefineFromNull};
     * therefore, {@code @Bottom} is also accepted.
     *
     * @param type the type to check
     * @param tree the tree to check
     */
    private void checkImplicitlyImmutableTypeError(AnnotatedTypeMirror type, Tree tree) {
        if (mutabilityTypeFactory.isImplicitlyImmutableType(type)
                && !type.hasAnnotation(mutabilityTypeFactory.IMMUTABLE)
                && !type.hasAnnotation(mutabilityTypeFactory.BOTTOM)) {
            reportInvalidAnnotationsOnUse(type, tree);
        }
    }

    /**
     * Reports an error if a class declaration uses an invalid mutability class bound.
     *
     * <p>Anonymous classes are validated through their creation expressions.
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

        if (!mutabilityTypeFactory.isValidClassBound(type)) {
            checker.reportError(tree, "class.bound.invalid", type);
            isValid = false;
        }
    }

    /**
     * Reports an error if a field declaration does not have exactly one assignability status.
     *
     * <p>This covers both instance fields and static fields.
     *
     * @param tree the tree to check
     */
    private void checkFieldAssignabilityDeclaration(Tree tree) {
        VariableElement field = getFieldElement(tree);
        if (field == null) {
            return;
        }
        if (!mutabilityTypeFactory.hasOneAndOnlyOneAssignabilityQualifier(field)) {
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
