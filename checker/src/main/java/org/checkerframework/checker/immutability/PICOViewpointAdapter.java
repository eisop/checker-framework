package org.checkerframework.checker.immutability;

import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.BOTTOM;
import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.IMMUTABLE;
import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.MUTABLE;
import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.POLY_MUTABLE;
import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.READONLY;
import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;

import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

public class PICOViewpointAdapter extends AbstractViewpointAdapter
        implements ExtendedViewpointAdapter {

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
        } else if (AnnotationUtils.areSame(declaredAnnotation, RECEIVER_DEPENDANT_MUTABLE)) {
            return receiverAnnotation;
        } else {
            throw new BugInCF("Unknown declared modifier: " + declaredAnnotation);
        }
    }

    //
    //    @Override
    //    protected AnnotatedTypeMirror combineAnnotationWithType(AnnotationMirror
    // receiverAnnotation, AnnotatedTypeMirror declared) {
    //        boolean prevRdm = declared.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE);
    //        AnnotatedTypeMirror raw =  super.combineAnnotationWithType(receiverAnnotation,
    // declared);
    //        if(prevRdm &&
    //
    // AnnotationUtils.containsSameByName(atypeFactory.getTypeDeclarationBounds(declared.getUnderlyingType()), MUTABLE)
    //                && (raw.hasAnnotation(IMMUTABLE) ||
    // raw.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE))) {
    //            raw.replaceAnnotation(MUTABLE);
    //        }
    //        return raw;
    //    }
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

    //
    //    @Override
    //    protected AnnotationMirror getModifier(AnnotatedTypeMirror atm, AnnotatedTypeFactory f) {
    //        return atm.getAnnotationInHierarchy(READONLY);
    //    }

    //    @Override
    //    protected <TypeFactory extends AnnotatedTypeFactory> AnnotationMirror
    // extractModifier(AnnotatedTypeMirror atm, TypeFactory f) {
    //        return null;
    //    }
}
