// An inference variable that appears in a throws clause resolves to RuntimeException only if
// every proper upper bound is a supertype of RuntimeException (JLS 18.4).  Here E also has the
// upper bound IOException from the assignment context, so E resolves to the glb of its upper
// bounds.  Inference used to pick RuntimeException when any upper bound was a supertype of it,
// and then reported the qualifier mismatch of that abandoned choice as type.arguments.not.inferred.

import org.checkerframework.checker.tainting.qual.Untainted;

import java.io.IOException;

public class ThrowsBoundResolution {
    static <E extends @Untainted Exception> E thrower() throws E {
        throw new Error();
    }

    void untaintedTarget() throws IOException {
        @Untainted IOException x = thrower();
    }

    void defaultTarget() throws IOException {
        IOException x = thrower();
    }

    static <E extends Exception> void voidThrower() throws E {}

    void noOtherBound() {
        // E resolves to RuntimeException, so nothing needs to be caught.
        ThrowsBoundResolution.<RuntimeException>voidThrower();
        voidThrower();
    }
}
