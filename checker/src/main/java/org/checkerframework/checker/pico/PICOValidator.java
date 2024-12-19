package org.checkerframework.checker.pico;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
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

import java.util.Objects;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/**
 * Enforce correct usage of immutability and assignability qualifiers. TODO @PolyMutable is only
 * used on constructor/method parameters or method return
 */
public class PICOValidator extends BaseTypeValidator {
    /** The type factory for the PICO checker */
    private final PICONoInitAnnotatedTypeFactory picoTypeFactory =
            (PICONoInitAnnotatedTypeFactory) checker.getTypeFactory();

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
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
        checkStaticReceiverDependentMutableError(type, tree);
        checkImplicitlyImmutableTypeError(type, tree);
        checkOnlyOneAssignabilityModifierOnField(tree);

        return super.visitDeclared(type, tree);
    }

    @Override
    protected boolean shouldCheckTopLevelDeclaredOrPrimitiveType(
            AnnotatedTypeMirror type, Tree tree) {
        // check top annotations in extends/implements clauses
        if ((tree.getKind() == Kind.IDENTIFIER || tree.getKind() == Kind.PARAMETERIZED_TYPE)
                && PICONoInitAnnotatedTypeFactory.PICOSuperClauseAnnotator.isSuperClause(
                        atypeFactory.getPath(tree))) {
            return true;
        }
        // allow RDM on mutable fields with enclosing class bounded with mutable
        if (tree instanceof VariableTree) {
            VariableElement element = TreeUtils.elementFromDeclaration((VariableTree) tree);
            if (element.getKind() == ElementKind.FIELD
                    && ElementUtils.enclosingTypeElement(element) != null) {
                @Immutable
                AnnotationMirrorSet enclosingBound =
                        atypeFactory.getTypeDeclarationBounds(
                                Objects.requireNonNull(ElementUtils.enclosingTypeElement(element))
                                        .asType());

                @Immutable
                AnnotationMirrorSet declaredBound =
                        atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType());

                if (AnnotationUtils.containsSameByName(declaredBound, picoTypeFactory.MUTABLE)
                        && type.hasAnnotation(picoTypeFactory.RECEIVER_DEPENDENT_MUTABLE)
                        && AnnotationUtils.containsSameByName(
                                enclosingBound, picoTypeFactory.MUTABLE)) {
                    return false;
                }
            }
        }
        // COPY from SUPER
        if (type.getKind() != TypeKind.DECLARED && !type.getKind().isPrimitive()) {
            return true;
        }
        // Do not call super because BaseTypeValidator will don't check local variable declaration
        return !TreeUtils.isExpressionTree(tree) || TreeUtils.isTypeTree(tree);
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
        checkOnlyOneAssignabilityModifierOnField(tree);
        return super.visitPrimitive(type, tree);
    }

    /**
     * Check that static fields do not have receiver-dependent mutable type.
     *
     * @param type the type to check
     * @param tree the tree to check
     */
    private void checkStaticReceiverDependentMutableError(AnnotatedTypeMirror type, Tree tree) {
        //        Element element;
        //        if (type instanceof AnnotatedDeclaredType) element =
        // ((AnnotatedDeclaredType)type).getUnderlyingType().asElement();
        //        else if (type instanceof AnnotatedArrayType) element =
        // ((AnnotatedArrayType)type).;
        ////        AnnotatedTypeMirror explicitATM = atypeFactory.fromElement(element);
        //        AnnotationMirrorSet declBound =
        // atypeFactory.getTypeDeclarationBounds(element.asType());
        if (!type.isDeclaration() // variables in static contexts and static fields use class
                // decl as enclosing type
                && PICOTypeUtil.inStaticScope(visitor.getCurrentPath())
                && !""
                        .contentEquals(
                                Objects.requireNonNull(
                                                TreePathUtil.enclosingClass(
                                                        visitor.getCurrentPath()))
                                        .getSimpleName())
                // Exclude @RDM usages in anonymous classes
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
     * Ensures the well-formdness in terms of assignability on a field. This covers both instance
     * fields and static fields.
     *
     * @param tree the tree to check
     */
    private void checkOnlyOneAssignabilityModifierOnField(Tree tree) {
        if (tree.getKind() == Kind.VARIABLE) {
            VariableTree variableTree = (VariableTree) tree;
            VariableElement variableElement = TreeUtils.elementFromDeclaration(variableTree);
            if (!PICOTypeUtil.hasOneAndOnlyOneAssignabilityQualifier(
                    variableElement, atypeFactory)) {
                checker.reportError(variableElement, "one.assignability.invalid", variableElement);
                isValid = false;
            }
        }
    }
}
