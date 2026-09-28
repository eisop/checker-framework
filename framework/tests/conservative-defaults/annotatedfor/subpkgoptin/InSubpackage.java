package afoptin.sub;

import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

// applyToSubpackages defaults to true, so package afoptin's @AnnotatedFor reaches this subpackage
// and its subtyping warnings are issued.
public class InSubpackage {
    void m() {
        // :: error: (assignment.type.incompatible)
        @SubQual Object o = new @SuperQual Object();
    }
}
