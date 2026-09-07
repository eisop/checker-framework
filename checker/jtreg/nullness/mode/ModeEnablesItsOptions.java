/*
 * @test
 *
 * @summary Test that -Amode=jspecify enables every option it names, not just -AonlyAnnotatedFor.
 * The uninitialized field is an error under -AonlyAnnotatedFor alone, and is accepted under the
 * mode, which also enables -AassumeInitialized.
 *
 * @compile/fail/ref=ModeEnablesItsOptions.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AonlyAnnotatedFor ModeEnablesItsOptions.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify ModeEnablesItsOptions.java
 */

import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class ModeEnablesItsOptions {
    Object o;
}
