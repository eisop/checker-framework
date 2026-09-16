package afoptin;

import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

public class InPackage {
    void m() {
        // :: error: (assignment.type.incompatible)
        @SubQual Object o = new @SuperQual Object();
    }
}
