package annotatedforscopelib;

import org.checkerframework.framework.qual.AnnotatedFor;

public class MethodScope {
    @AnnotatedFor("nullness")
    public static Object annotated() {
        return "";
    }

    public static Object plain() {
        return "";
    }
}
