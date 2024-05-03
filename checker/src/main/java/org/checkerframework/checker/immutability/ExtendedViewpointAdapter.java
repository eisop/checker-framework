package org.checkerframework.checker.immutability;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.ViewpointAdapter;

import javax.lang.model.element.AnnotationMirror;

public interface ExtendedViewpointAdapter extends ViewpointAdapter {
    AnnotatedTypeMirror rawCombineAnnotationWithType(
            AnnotationMirror anno, AnnotatedTypeMirror type);

    AnnotationMirror rawCombineAnnotationWithAnnotation(
            AnnotationMirror anno, AnnotationMirror type);
}
