package afoptout;

import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

// Opting out of subpackages does not opt the annotated package itself out, so this code is in an
// @AnnotatedFor scope and its subtyping warnings are issued.
public class InPackage {
    void m() {
        // :: error: (assignment.type.incompatible)
        @SubQual Object o = new @SuperQual Object();
    }
}
