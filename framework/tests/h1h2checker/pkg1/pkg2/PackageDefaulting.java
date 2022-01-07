package pkg1.pkg2;

import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;

/* Default from package pkg1 is not applied to subpackages. */
public class PackageDefaulting {
    void m(@H1S1 Object p1, @H1S2 Object p2) {
        Object l1 = p1;
        Object l2 = p2;
    }
}
