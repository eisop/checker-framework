package org.checkerframework.checker.pico;

import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

import javax.lang.model.element.AnnotationMirror;

/**
 * Viewpoint adapter for PICO mutability qualifiers.
 *
 * <p>Most PICO qualifiers are stable under viewpoint adaptation. Only
 * {@code @ReceiverDependentMutable} depends on the receiver: mutable and immutable receivers
 * preserve their own qualifier, while a readonly receiver loses the precise mutability information
 * and adapts to {@code @PICOLost}.
 */
public class PICOViewpointAdapter extends AbstractViewpointAdapter {
    /** The PICO type factory. */
    private final PICONoInitAnnotatedTypeFactory picoTypeFactory;

    /**
     * Create a new {@link PICOViewpointAdapter}.
     *
     * @param atypeFactory the type factory
     */
    public PICOViewpointAdapter(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
        picoTypeFactory = (PICONoInitAnnotatedTypeFactory) atypeFactory;
    }

    @Override
    protected AnnotationMirror extractAnnotationMirror(AnnotatedTypeMirror atm) {
        return atm.getAnnotationInHierarchy(picoTypeFactory.READONLY);
    }

    @Override
    protected AnnotationMirror combineAnnotationWithAnnotation(
            AnnotationMirror receiverAnnotation, AnnotationMirror declaredAnnotation) {
        if (declaredAnnotation == null) {
            declaredAnnotation = picoTypeFactory.READONLY;
        }

        if (AnnotationUtils.areSame(
                declaredAnnotation, picoTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
            if (AnnotationUtils.areSame(receiverAnnotation, picoTypeFactory.READONLY)) {
                return picoTypeFactory.LOST;
            }
            return receiverAnnotation;
        }

        if (isFixedQualifier(declaredAnnotation)) {
            return declaredAnnotation;
        }

        throw new BugInCF("Unknown declared qualifier: " + declaredAnnotation);
    }

    /**
     * Returns true if {@code annotation} is a PICO qualifier that is unchanged by viewpoint
     * adaptation.
     *
     * @param annotation the annotation to test
     * @return true if viewpoint adaptation returns {@code annotation} unchanged
     */
    private boolean isFixedQualifier(AnnotationMirror annotation) {
        return AnnotationUtils.areSame(annotation, picoTypeFactory.READONLY)
                || AnnotationUtils.areSame(annotation, picoTypeFactory.MUTABLE)
                || AnnotationUtils.areSame(annotation, picoTypeFactory.IMMUTABLE)
                || AnnotationUtils.areSame(annotation, picoTypeFactory.BOTTOM)
                || AnnotationUtils.areSame(annotation, picoTypeFactory.POLY_MUTABLE)
                || AnnotationUtils.areSame(annotation, picoTypeFactory.LOST);
    }
}
