package afoptout.sub.nested;

import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

// The enclosing package opts out of all subpackages, including transitively nested ones.
public class InNestedSubpackage {
    void m() {
        @SubQual Object o = new @SuperQual Object();
    }
}
