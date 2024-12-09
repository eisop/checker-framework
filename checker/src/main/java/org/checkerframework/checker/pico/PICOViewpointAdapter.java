package org.checkerframework.checker.pico;

import static org.checkerframework.checker.pico.PICOAnnotationMirrorHolder.BOTTOM;
import static org.checkerframework.checker.pico.PICOAnnotationMirrorHolder.IMMUTABLE;
import static org.checkerframework.checker.pico.PICOAnnotationMirrorHolder.LOST;
import static org.checkerframework.checker.pico.PICOAnnotationMirrorHolder.MUTABLE;
import static org.checkerframework.checker.pico.PICOAnnotationMirrorHolder.POLY_MUTABLE;
import static org.checkerframework.checker.pico.PICOAnnotationMirrorHolder.READONLY;
import static org.checkerframework.checker.pico.PICOAnnotationMirrorHolder.RECEIVER_DEPENDENT_MUTABLE;

import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

public class PICOViewpointAdapter extends AbstractViewpointAdapter {

    public PICOViewpointAdapter(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
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
