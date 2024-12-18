package org.checkerframework.checker.pico;

import org.checkerframework.checker.pico.qual.Lost;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;
import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

public class PICOViewpointAdapter extends AbstractViewpointAdapter {
    /** The {@link Readonly}, {@link ReceiverDependentMutable} and {@link Lost} annotation. */
    private final AnnotationMirror READONLY,
            MUTABLE,
            IMMUTABLE,
            BOTTOM,
            POLY_MUTABLE,
            RECEIVER_DEPENDENT_MUTABLE,
            LOST;

    public PICOViewpointAdapter(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
        READONLY = ((PICONoInitAnnotatedTypeFactory) atypeFactory).READONLY;
        MUTABLE = ((PICONoInitAnnotatedTypeFactory) atypeFactory).MUTABLE;
        IMMUTABLE = ((PICONoInitAnnotatedTypeFactory) atypeFactory).IMMUTABLE;
        BOTTOM = ((PICONoInitAnnotatedTypeFactory) atypeFactory).BOTTOM;
        POLY_MUTABLE = ((PICONoInitAnnotatedTypeFactory) atypeFactory).POLY_MUTABLE;
        RECEIVER_DEPENDENT_MUTABLE =
                ((PICONoInitAnnotatedTypeFactory) atypeFactory).RECEIVER_DEPENDENT_MUTABLE;
        LOST = ((PICONoInitAnnotatedTypeFactory) atypeFactory).LOST;
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
        return atm.getAnnotationInHierarchy(READONLY);
    }

    @Override
    protected AnnotationMirror combineAnnotationWithAnnotation(
            AnnotationMirror receiverAnnotation, AnnotationMirror declaredAnnotation) {
        if (AnnotationUtils.areSame(declaredAnnotation, READONLY)) {
            return READONLY;
        } else if (AnnotationUtils.areSame(declaredAnnotation, MUTABLE)) {
            return MUTABLE;
        } else if (AnnotationUtils.areSame(declaredAnnotation, IMMUTABLE)) {
            return IMMUTABLE;
        } else if (AnnotationUtils.areSame(declaredAnnotation, BOTTOM)) {
            return BOTTOM;
        } else if (AnnotationUtils.areSame(declaredAnnotation, POLY_MUTABLE)) {
            return POLY_MUTABLE;
        } else if (AnnotationUtils.areSame(declaredAnnotation, RECEIVER_DEPENDENT_MUTABLE)) {
            // @Readonly |> @ReceiverDependentMutable = @Lost
            if (AnnotationUtils.areSame(receiverAnnotation, READONLY)) {
                return LOST;
            } else {
                return receiverAnnotation;
            }
        } else {
            throw new BugInCF("Unknown declared modifier: " + declaredAnnotation);
        }
    }
}
