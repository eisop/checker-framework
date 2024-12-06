package viewpointtest;

import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;

import viewpointtest.quals.Lost;
import viewpointtest.quals.ReceiverDependentQual;
import viewpointtest.quals.Top;

/** The viewpoint adapter for the Viewpoint Test Checker. */
public class ViewpointTestViewpointAdapter extends AbstractViewpointAdapter {

    /** The {@link Top}, {@link ReceiverDependentQual} and {@link Lost} annotation. */
    private final AnnotationMirror TOP, RECEIVERDEPENDENTQUAL, LOST;

    /**
     * The class constructor.
     *
     * @param atypeFactory the type factory to use
     */
    public ViewpointTestViewpointAdapter(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
        TOP = ((ViewpointTestAnnotatedTypeFactory) atypeFactory).TOP;
        RECEIVERDEPENDENTQUAL =
                AnnotationBuilder.fromClass(
                        atypeFactory.getElementUtils(), ReceiverDependentQual.class);
        LOST = ((ViewpointTestAnnotatedTypeFactory) atypeFactory).LOST;
    }

    @Override
    protected AnnotationMirror extractAnnotationMirror(AnnotatedTypeMirror atm) {
        return atm.getAnnotationInHierarchy(TOP);
    }

    @Override
    protected AnnotationMirror combineAnnotationWithAnnotation(
            AnnotationMirror receiverAnnotation, AnnotationMirror declaredAnnotation) {

        if (AnnotationUtils.areSame(declaredAnnotation, RECEIVERDEPENDENTQUAL)) {
            if (AnnotationUtils.areSame(receiverAnnotation, TOP)) {
                return LOST;
            } else {
                return receiverAnnotation;
            }
        }
        return declaredAnnotation;
    }
}
