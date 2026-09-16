/*
 * @test
 *
 * @summary Test that -Amode=jspecify enables -AjspecifyUnrecognizedLocations.  The annotation on
 * the class declaration is accepted without the mode and rejected with it.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AonlyAnnotatedFor ModeEnablesUnrecognizedLocations.java
 * @compile/fail/ref=ModeEnablesUnrecognizedLocations.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify ModeEnablesUnrecognizedLocations.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
@Nullable public class ModeEnablesUnrecognizedLocations {}
