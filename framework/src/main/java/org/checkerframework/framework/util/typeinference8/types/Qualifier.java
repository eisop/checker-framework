package org.checkerframework.framework.util.typeinference8.types;

import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;

import javax.lang.model.element.AnnotationMirror;

/** A wrapper around an {@link AnnotationMirror}. */
public class Qualifier extends AbstractQualifier {

    /** The annotation. */
    private final AnnotationMirror annotation;

    /**
     * A wrapper around an {@link AnnotationMirror}.
     *
     * @param annotation the annotation
     * @param context the context
     */
    protected Qualifier(AnnotationMirror annotation, Java8InferenceContext context) {
        super(annotation, context);
        this.annotation = annotation;
    }

    /**
     * Returns the annotation
     *
     * @return the annotation
     */
    public AnnotationMirror getAnnotation() {
        return annotation;
    }

    @Override
    public AnnotationMirror getInstantiation() {
        return annotation;
    }

    @Override
    public String toString() {
        return annotation.toString();
    }
}
