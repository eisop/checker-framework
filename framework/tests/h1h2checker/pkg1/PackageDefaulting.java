package pkg1;

import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;

/* Default from package pkg1 applies within the package. */
public class PackageDefaulting {
    void m(@H1S1 Object p1, @H1S2 Object p2) {
        Object l1 = p1;
        // :: error: (assignment.type.incompatible)
        Object l2 = p2;
    }
}
