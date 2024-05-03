import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;

import java.util.Date;

// In these two cases, types are not applied computed annotations:
// 1) Has explicit annotation, e.g. d2 and d3
// 2) Has bound annotation on Element, e.g. d. It can be from source file or stub file(jdk.astub)
public class StaticFields {
    // (this case again) Not implicitly immutable type - Handled by PICOTreeAnnotator on declaration
    // Update: d is equivalent to having explicit annotations now. TreeAnnotators belong to
    // addComputedAnnotations()
    // phase, and they don't have any effect now(provided those TreeAnnotators don't always
    // replaceAnnotations, and
    // this is true for PICOTreeAnnotators: they all repect existing annotations from source and
    // elements)
    // Update2: d no longer gets inheritted @ReceiverDependantMutable anymore. It goes back to
    // original
    // implementation - PICOTreeAnnotator adds @Mutable to non-implicitly immutable type. This is
    // the result
    // of not inheritting @ReceiverDependantMutable to the usage because it creates unexpected
    // noises.
    // As a result, d has no annotation on it, and it falls through to PICOTreeAnnotator and gets
    // added
    // @Mutable.
    static Date d;
    static Integer i; // Implicitly immutable type - Handled by PICOImplicitsTypeAnnotator
    static @Mutable Date d2;
    static @Immutable Date d3;

    static {
        // (not the case anymore) new instance creation also has @ReceiverDependantMutable type
        // which is from element declaration in stub file(bound)
        d = new Date(); // (this case again) Handled by addComputedTypeAnnotations(Element,
        // AnnotatedTypeMirror) when used
        i = 2; // Handled by PICOImplicitsTypeAnnotator when used
        d2 = new @Mutable Date();
        d3 = new @Immutable Date();
    }
}
