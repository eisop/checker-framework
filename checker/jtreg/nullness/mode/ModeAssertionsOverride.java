/*
 * @test
 *
 * @summary Test that -AassumeAssertions overrides the "enabled" value that -Amode=jspecify adds,
 * including when it is written as one of the deprecated options that it replaced.  The dereference
 * is accepted under the mode alone, which assumes the assertion has run.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify ModeAssertionsOverride.java
 * @compile/fail/ref=ModeAssertionsOverride.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify -AassumeAssertions=disabled ModeAssertionsOverride.java
 * @compile/fail/ref=ModeAssertionsOverride.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify -AassumeAssertions=neither ModeAssertionsOverride.java
 * @compile/fail/ref=ModeAssertionsOverrideDeprecated.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify -AassumeAssertionsAreDisabled ModeAssertionsOverride.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class ModeAssertionsOverride {
    void assertionRefines(@Nullable Object o) {
        assert o != null;
        o.toString();
    }
}
