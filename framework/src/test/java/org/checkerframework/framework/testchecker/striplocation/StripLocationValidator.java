package org.checkerframework.framework.testchecker.striplocation;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.WildcardTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.striplocation.quals.StripTop;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

/**
 * A validator that additionally enforces a rule {@code @TargetLocations} cannot express: {@link
 * StripTop}, the checker's whole-system top qualifier, carries no {@code @TargetLocations} of its
 * own, so {@link BaseTypeVisitor#annotationsDisallowedAtLocation} and {@link
 * #annotationsDisallowedAtWildcardBound} never flag or strip it, no matter where it appears. This
 * validator additionally forbids writing it explicitly at a lower-bound location -- it may only
 * arrive there through defaulting -- by inspecting the declaration/bound tree directly, exercising
 * the {@link #additionalAnnotationsToStripFromTypeVariableBound} and {@link
 * #additionalAnnotationsToStripFromWildcardBound} hooks.
 *
 * <p>Because an explicit {@code @StripTop} lower bound is incompatible with an explicit non-top
 * upper bound, stripping it (rather than merely reporting it) has an observable effect: it avoids a
 * {@code bound.type.incompatible} cascade, just as the {@code @TargetLocations}-based mechanism
 * does for {@link org.checkerframework.framework.testchecker.striplocation.quals.StripUpperOnly}.
 */
public class StripLocationValidator extends BaseTypeValidator {

    /**
     * Creates a new StripLocationValidator.
     *
     * @param checker the checker
     * @param visitor the visitor
     * @param atypeFactory the type factory
     */
    public StripLocationValidator(
            BaseTypeChecker checker,
            StripLocationVisitor visitor,
            AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
    }

    @Override
    protected List<AnnotationMirror> additionalAnnotationsToStripFromTypeVariableBound(
            AnnotatedTypeVariable type,
            Tree tree,
            AnnotatedTypeMirror bound,
            TypeUseLocation location) {
        if (location != TypeUseLocation.LOWER_BOUND
                || !(tree instanceof TypeParameterTree)
                || !atypeFactory.containsSameByClass(
                        TreeUtils.annotationsFromTree((TypeParameterTree) tree), StripTop.class)) {
            return Collections.emptyList();
        }
        checker.reportError(tree, "explicit.striptop.on.lowerbound");
        return Collections.singletonList(bound.getAnnotation(StripTop.class));
    }

    @Override
    protected List<AnnotationMirror> additionalAnnotationsToStripFromWildcardBound(
            AnnotatedWildcardType type,
            Tree tree,
            AnnotatedTypeMirror bound,
            Set<TypeUseLocation> allowedLocations) {
        if (!allowedLocations.contains(TypeUseLocation.LOWER_BOUND)
                || !atypeFactory.containsSameByClass(
                        explicitLowerBoundAnnotations(tree), StripTop.class)) {
            return Collections.emptyList();
        }
        checker.reportError(tree, "explicit.striptop.on.lowerbound");
        return Collections.singletonList(bound.getAnnotation(StripTop.class));
    }

    /**
     * Returns the annotations explicitly written at a wildcard's lower-bound source position: a
     * primary annotation directly on {@code ?} for {@code ? extends X} (where {@code tree} is the
     * {@code AnnotatedTypeTree} wrapping the wildcard), or an annotation on the bound type itself
     * for {@code ? super X} (where the bound tree may be annotated).
     *
     * @param tree the tree passed to {@link #additionalAnnotationsToStripFromWildcardBound}
     * @return the annotations explicitly written at the wildcard's lower-bound source position
     */
    private static List<? extends AnnotationMirror> explicitLowerBoundAnnotations(Tree tree) {
        if (tree instanceof AnnotatedTypeTree
                && ((AnnotatedTypeTree) tree).getUnderlyingType().getKind()
                        == Tree.Kind.EXTENDS_WILDCARD) {
            return TreeUtils.annotationsFromTree((AnnotatedTypeTree) tree);
        }
        if (tree instanceof WildcardTree && tree.getKind() == Tree.Kind.SUPER_WILDCARD) {
            Tree boundTree = ((WildcardTree) tree).getBound();
            if (boundTree instanceof AnnotatedTypeTree) {
                return TreeUtils.annotationsFromTree((AnnotatedTypeTree) boundTree);
            }
        }
        return Collections.emptyList();
    }
}
