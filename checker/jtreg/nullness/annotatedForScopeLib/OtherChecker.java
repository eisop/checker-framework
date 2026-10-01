package annotatedforscopelib;

import org.checkerframework.framework.qual.AnnotatedFor;

/** Annotated for a different checker, so it is unannotated as far as nullness is concerned. */
@AnnotatedFor("regex")
public class OtherChecker {
    public static Object get() {
        return "";
    }
}
