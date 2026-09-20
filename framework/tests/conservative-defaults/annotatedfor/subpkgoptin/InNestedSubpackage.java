package afoptin.sub.nested;

import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

// applyToSubpackages defaults to true, so package afoptin's @AnnotatedFor reaches transitively
// nested subpackages.
public class InNestedSubpackage {
    void m() {
        // :: error: (assignment.type.incompatible)
        @SubQual Object o = new @SuperQual Object();
    }
}
