package annotatedforscopelib;

import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class AnnotatedOuter {
    /** Not itself annotated; the enclosing class is. */
    public static class Nested {
        public static Object get() {
            return "";
        }
    }
}
