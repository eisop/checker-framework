package org.checkerframework.checker.immutability;

import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

public class PICOViewpointAdapter extends AbstractViewpointAdapter
        implements ExtendedViewpointAdapter {
    public final AnnotationMirror MUTABLE;
    public final AnnotationMirror READONLY;
    public final AnnotationMirror IMMUTABLE;
    public final AnnotationMirror POLY_MUTABLE;
    public final AnnotationMirror RECEIVER_DEPENDANT_MUTABLE;
    public final AnnotationMirror BOTTOM;

    public PICOViewpointAdapter(PICONoInitAnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
        MUTABLE = atypeFactory.MUTABLE;
        READONLY = atypeFactory.READONLY;
        IMMUTABLE = atypeFactory.IMMUTABLE;
        POLY_MUTABLE = atypeFactory.POLY_MUTABLE;
        RECEIVER_DEPENDANT_MUTABLE = atypeFactory.RECEIVER_DEPENDANT_MUTABLE;
        BOTTOM = atypeFactory.BOTTOM;
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
        } else if (AnnotationUtils.areSame(declaredAnnotation, RECEIVER_DEPENDANT_MUTABLE)) {
            return receiverAnnotation;
        } else {
            throw new BugInCF("Unknown declared modifier: " + declaredAnnotation);
        }
    }

    @Override
    public AnnotatedTypeMirror rawCombineAnnotationWithType(
            AnnotationMirror anno, AnnotatedTypeMirror type) {
        //        System.err.println("VPA: " + anno + " ->" + type);
        return combineAnnotationWithType(anno, type);
    }

    @Override
    public AnnotationMirror rawCombineAnnotationWithAnnotation(
            AnnotationMirror anno, AnnotationMirror type) {
        //        System.err.println("VPA: " + anno + " ->" + type);
        return combineAnnotationWithAnnotation(anno, type);
    }
}
