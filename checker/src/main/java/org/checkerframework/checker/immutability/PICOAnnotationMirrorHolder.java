package org.checkerframework.checker.immutability;

import org.checkerframework.checker.immutability.qual.Bottom;
import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.PolyMutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.javacutil.AnnotationBuilder;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

/** A holder class that holds AnnotationMirrors that are shared by PICO and PICOInfer. */
public class PICOAnnotationMirrorHolder {

    public static AnnotationMirror READONLY;
    public static AnnotationMirror MUTABLE;
    public static AnnotationMirror POLY_MUTABLE;
    public static AnnotationMirror RECEIVER_DEPENDANT_MUTABLE;
    public static AnnotationMirror IMMUTABLE;
    public static AnnotationMirror BOTTOM;
    public static AnnotationMirror COMMITED;

    public static void init(SourceChecker checker) {
        Elements elements = checker.getElementUtils();
        READONLY = AnnotationBuilder.fromClass(elements, Readonly.class);
        MUTABLE = AnnotationBuilder.fromClass(elements, Mutable.class);
        POLY_MUTABLE = AnnotationBuilder.fromClass(elements, PolyMutable.class);
        RECEIVER_DEPENDANT_MUTABLE =
                AnnotationBuilder.fromClass(elements, ReceiverDependantMutable.class);
        IMMUTABLE = AnnotationBuilder.fromClass(elements, Immutable.class);
        BOTTOM = AnnotationBuilder.fromClass(elements, Bottom.class);

        COMMITED = AnnotationBuilder.fromClass(elements, Initialized.class);
    }
}
