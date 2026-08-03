package org.checkerframework.framework.testchecker.striplocation;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.striplocation.quals.StripBottom;
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
 * StripBottom} carries no {@code @TargetLocations} of its own (it is the bottom of the whole type
 * system, not a narrow location-restricted qualifier), so {@link
 * BaseTypeVisitor#annotationsDisallowedAtLocation} and {@link
 * #annotationsDisallowedAtWildcardBound} never flag or strip it, no matter where it appears. This
 * validator additionally forbids writing it explicitly at a lower-bound location -- it may only
 * arrive there through defaulting -- which requires looking at the declaration/bound tree directly
 * to distinguish the two cases. This exercises the {@code declTree} parameter of {@link
 * #stripInvalidLocationQualifiersFromBounds} and the {@link
 * #additionalAnnotationsToStripFromWildcardBound} hook.
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
    protected void stripInvalidLocationQualifiersFromBounds(
            AnnotatedTypeVariable type,
            AnnotatedTypeMirror upperBound,
            AnnotatedTypeMirror lowerBound,
            Tree declTree) {
        super.stripInvalidLocationQualifiersFromBounds(type, upperBound, lowerBound, declTree);
        if (!(declTree instanceof TypeParameterTree)
                || !atypeFactory.containsSameByClass(
                        TreeUtils.annotationsFromTree((TypeParameterTree) declTree),
                        StripBottom.class)) {
            return;
        }
        checker.reportError(declTree, "explicit.stripbottom.on.lowerbound");
        lowerBound.removeAnnotation(lowerBound.getAnnotation(StripBottom.class));
        atypeFactory.addDefaultAnnotations(type);
    }

    @Override
    protected List<AnnotationMirror> additionalAnnotationsToStripFromWildcardBound(
            AnnotatedWildcardType type,
            Tree tree,
            AnnotatedTypeMirror bound,
            Set<TypeUseLocation> allowedLocations) {
        if (!allowedLocations.contains(TypeUseLocation.LOWER_BOUND)
                || !(tree instanceof AnnotatedTypeTree)
                || !atypeFactory.containsSameByClass(
                        TreeUtils.annotationsFromTree((AnnotatedTypeTree) tree),
                        StripBottom.class)) {
            return Collections.emptyList();
        }
        checker.reportError(tree, "explicit.stripbottom.on.lowerbound");
        return Collections.singletonList(bound.getAnnotation(StripBottom.class));
    }
}
