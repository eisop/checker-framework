package org.checkerframework.checker.pico;

import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

/** A {@link AbstractViewpointAdapter} for the PICO checker. */
public class PICOViewpointAdapter extends AbstractViewpointAdapter {
    /** The PICO type factory. */
    private PICONoInitAnnotatedTypeFactory picoTypeFactory;

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
    protected boolean shouldAdaptMember(AnnotatedTypeMirror type, Element element) {
        if (!(type.getKind() == TypeKind.DECLARED || type.getKind() == TypeKind.ARRAY)) {
            return false;
        }
        return super.shouldAdaptMember(type, element);
    }

    @Override
    protected AnnotationMirror extractAnnotationMirror(AnnotatedTypeMirror atm) {
        return atm.getAnnotationInHierarchy(picoTypeFactory.READONLY);
    }

    @Override
    protected AnnotationMirror combineAnnotationWithAnnotation(
            AnnotationMirror receiverAnnotation, AnnotationMirror declaredAnnotation) {
        if (AnnotationUtils.areSame(declaredAnnotation, picoTypeFactory.READONLY)) {
            return picoTypeFactory.READONLY;
        } else if (AnnotationUtils.areSame(declaredAnnotation, picoTypeFactory.MUTABLE)) {
            return picoTypeFactory.MUTABLE;
        } else if (AnnotationUtils.areSame(declaredAnnotation, picoTypeFactory.IMMUTABLE)) {
            return picoTypeFactory.IMMUTABLE;
        } else if (AnnotationUtils.areSame(declaredAnnotation, picoTypeFactory.BOTTOM)) {
            return picoTypeFactory.BOTTOM;
        } else if (AnnotationUtils.areSame(declaredAnnotation, picoTypeFactory.POLY_MUTABLE)) {
            return picoTypeFactory.POLY_MUTABLE;
        } else if (AnnotationUtils.areSame(
                declaredAnnotation, picoTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
            // @Readonly |> @ReceiverDependentMutable = @Lost
            if (AnnotationUtils.areSame(receiverAnnotation, picoTypeFactory.READONLY)) {
                return picoTypeFactory.LOST;
            } else {
                return receiverAnnotation;
            }
        } else {
            throw new BugInCF("Unknown declared modifier: " + declaredAnnotation);
        }
    }
}
