package org.checkerframework.checker.mutability;

import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

import javax.lang.model.element.AnnotationMirror;

/**
 * Viewpoint adapter for mutability qualifiers.
 *
 * <p>Most mutability qualifiers are stable under viewpoint adaptation. Only
 * {@code @ReceiverDependentMutable} depends on the receiver: mutable and immutable receivers
 * preserve their own qualifier, while a readonly receiver loses the precise mutability information
 * and adapts to {@code @MutabilityLost}.
 */
public class MutabilityViewpointAdapter extends AbstractViewpointAdapter {
    /** The mutability type factory. */
    private final MutabilityNoInitAnnotatedTypeFactory mutabilityTypeFactory;

    /**
     * Create a new {@link MutabilityViewpointAdapter}.
     *
     * @param atypeFactory the type factory
     */
    public MutabilityViewpointAdapter(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
        mutabilityTypeFactory = (MutabilityNoInitAnnotatedTypeFactory) atypeFactory;
    }

    @Override
    protected AnnotationMirror extractAnnotationMirror(AnnotatedTypeMirror atm) {
        return atm.getAnnotationInHierarchy(mutabilityTypeFactory.READONLY);
    }

    @Override
    protected AnnotationMirror combineAnnotationWithAnnotation(
            AnnotationMirror receiverAnnotation, AnnotationMirror declaredAnnotation) {
        if (declaredAnnotation == null) {
            declaredAnnotation = mutabilityTypeFactory.READONLY;
        }

        if (AnnotationUtils.areSame(
                declaredAnnotation, mutabilityTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
            if (AnnotationUtils.areSame(receiverAnnotation, mutabilityTypeFactory.READONLY)) {
                return mutabilityTypeFactory.LOST;
            }
            return receiverAnnotation;
        }

        if (isFixedQualifier(declaredAnnotation)) {
            return declaredAnnotation;
        }

        throw new BugInCF("Unknown declared qualifier: " + declaredAnnotation);
    }

    /**
     * Returns true if {@code annotation} is a mutability qualifier that is unchanged by viewpoint
     * adaptation.
     *
     * @param annotation the annotation to test
     * @return true if viewpoint adaptation returns {@code annotation} unchanged
     */
    private boolean isFixedQualifier(AnnotationMirror annotation) {
        return AnnotationUtils.areSame(annotation, mutabilityTypeFactory.READONLY)
                || AnnotationUtils.areSame(annotation, mutabilityTypeFactory.MUTABLE)
                || AnnotationUtils.areSame(annotation, mutabilityTypeFactory.IMMUTABLE)
                || AnnotationUtils.areSame(annotation, mutabilityTypeFactory.BOTTOM)
                || AnnotationUtils.areSame(annotation, mutabilityTypeFactory.POLY_MUTABLE)
                || AnnotationUtils.areSame(annotation, mutabilityTypeFactory.LOST);
    }
}
