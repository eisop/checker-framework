// Regression test: AnnotatedTypeCopier.visitExecutable used to alias (rather than copy) the
// vararg type when deep-copying an executable type, so deepCopy() did not produce a fully
// independent type. The shared vararg subtree was then re-defaulted in place. This is benign
// while cached types are mutable, but corrupts a shared cached value once cache masters are
// frozen; with that enforcement it crashed with "Attempted to mutate a frozen
// AnnotatedTypeMirror". Calling JDK vararg methods (whose executable types are cached and
// re-defaulted) reproduces it. This test should type-check with no errors.

import java.util.Arrays;
import java.util.List;

public class VarargCacheAliasing {
    void use(String s) throws Exception {
        List<String> xs = Arrays.asList(s, s, s);
        List<String> ys = Arrays.asList(s);
        String f = String.format("%s %s", s, s);
        java.lang.reflect.Method m = VarargCacheAliasing.class.getMethod("use", String.class);
    }
}
