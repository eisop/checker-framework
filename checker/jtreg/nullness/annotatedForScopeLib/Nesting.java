package annotatedforscopelib;

import org.checkerframework.framework.qual.AnnotatedFor;

/** Not itself annotated, so the annotation on the nested class is the only one in scope. */
public class Nesting {
    @AnnotatedFor("nullness")
    public static class AnnotatedNested {
        public static Object get() {
            return "";
        }
    }

    public static class PlainNested {
        public static Object get() {
            return "";
        }
    }
}
