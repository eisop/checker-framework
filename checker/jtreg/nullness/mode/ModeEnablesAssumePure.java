/*
 * @test
 *
 * @summary Test that -Amode=jspecify enables -AassumePure.  The refinement of the field is
 * discarded by the call to a method that is not known to be side-effect-free, and survives it
 * under the mode, which assumes every called method is pure.
 *
 * @compile/fail/ref=ModeEnablesAssumePure.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AonlyAnnotatedFor ModeEnablesAssumePure.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify ModeEnablesAssumePure.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class ModeEnablesAssumePure {
    @Nullable Object f;

    void sideEffect() {}

    void refinementSurvivesCall() {
        if (f != null) {
            sideEffect();
            f.toString();
        }
    }
}
