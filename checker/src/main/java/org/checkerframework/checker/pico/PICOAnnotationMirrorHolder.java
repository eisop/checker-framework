package org.checkerframework.checker.pico;

import org.checkerframework.checker.pico.qual.Bottom;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Lost;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.javacutil.AnnotationBuilder;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

/** A holder class that holds AnnotationMirrors that are shared by PICO and PICOInfer. */
public class PICOAnnotationMirrorHolder {

    public static AnnotationMirror READONLY;
    public static AnnotationMirror MUTABLE;
    public static AnnotationMirror POLY_MUTABLE;
    public static AnnotationMirror RECEIVER_DEPENDENT_MUTABLE;
    public static AnnotationMirror IMMUTABLE;
    public static AnnotationMirror LOST;
    public static AnnotationMirror BOTTOM;

    public static void init(SourceChecker checker) {
        Elements elements = checker.getElementUtils();
        READONLY = AnnotationBuilder.fromClass(elements, Readonly.class);
        MUTABLE = AnnotationBuilder.fromClass(elements, Mutable.class);
        POLY_MUTABLE = AnnotationBuilder.fromClass(elements, PolyMutable.class);
        RECEIVER_DEPENDENT_MUTABLE =
                AnnotationBuilder.fromClass(elements, ReceiverDependentMutable.class);
        IMMUTABLE = AnnotationBuilder.fromClass(elements, Immutable.class);
        LOST = AnnotationBuilder.fromClass(elements, Lost.class);
        BOTTOM = AnnotationBuilder.fromClass(elements, Bottom.class);
    }
}
