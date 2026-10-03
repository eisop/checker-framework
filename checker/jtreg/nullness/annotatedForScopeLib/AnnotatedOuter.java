package annotatedforscopelib;

import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.UnannotatedFor;

@AnnotatedFor("nullness")
public class AnnotatedOuter {
    /** Not itself annotated; the enclosing class is. */
    public static class Nested {
        public static Object get() {
            return "";
        }
    }

    /** Opts back out of the enclosing class's {@code @AnnotatedFor}. */
    @UnannotatedFor("nullness")
    public static class OptedOut {
        public static Object get() {
            return "";
        }
    }
}
