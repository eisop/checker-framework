package afoptout.sub;

import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

// Package afoptout sets applyToSubpackages=false, so this code is outside any @AnnotatedFor scope
// and conservative defaults suppress its warnings. No error is expected below.
public class InSubpackage {
    void m() {
        @SubQual Object o = new @SuperQual Object();
    }
}
