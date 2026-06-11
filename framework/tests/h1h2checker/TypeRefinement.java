import org.checkerframework.framework.testchecker.h1h2checker.quals.*;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Invalid;

public class TypeRefinement {
    // :: warning: (cast.unsafe.constructor.invocation)
    @H1Top Object o = new @H1S1 Object();
    // :: error: (type.invalid)
    @H1Top Object o2 = new @H1Invalid Object();
    // :: error: (type.invalid)
    @H1Top Object o3 = getH1Invalid();

    // :: error: (type.invalid)
    @H1Invalid Object getH1Invalid() {
        // :: error: (type.invalid)
        return new @H1Invalid Object();
    }
}
