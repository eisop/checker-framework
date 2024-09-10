package viewpointtest;

import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;

import viewpointtest.quals.A;
import viewpointtest.quals.C;
import viewpointtest.quals.ReceiverDependentQual;
import viewpointtest.quals.Top;

public class ViewpointTestViewpointAdapter extends AbstractViewpointAdapter {

    private final AnnotationMirror TOP, RECEIVERDEPENDENTQUAL, A, C;

    /**
     * The class constructor.
     *
     * @param atypeFactory the type factory to use
     */
    public ViewpointTestViewpointAdapter(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
        TOP = AnnotationBuilder.fromClass(atypeFactory.getElementUtils(), Top.class);
        RECEIVERDEPENDENTQUAL =
                AnnotationBuilder.fromClass(
                        atypeFactory.getElementUtils(), ReceiverDependentQual.class);
        A = AnnotationBuilder.fromClass(atypeFactory.getElementUtils(), A.class);
        C = AnnotationBuilder.fromClass(atypeFactory.getElementUtils(), C.class);
    }

    @Override
    protected AnnotationMirror extractAnnotationMirror(AnnotatedTypeMirror atm) {
        return atm.getAnnotationInHierarchy(TOP);
    }

    @Override
    protected AnnotationMirror combineAnnotationWithAnnotation(
            AnnotationMirror receiverAnnotation, AnnotationMirror declaredAnnotation) {

        if (AnnotationUtils.areSame(declaredAnnotation, RECEIVERDEPENDENTQUAL)) {
            return receiverAnnotation;
        } else if (AnnotationUtils.areSame(declaredAnnotation, C)) {
            if (AnnotationUtils.areSame(receiverAnnotation, TOP)) {
                return TOP;
            } else {
                return C;
            }
        } else {
            return declaredAnnotation;
        }
    }
}
