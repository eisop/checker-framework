/*
 * @test
 * @summary Test for EISOP Issue #321
 * @compile/fail/ref=Issue321.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Issue321.java -Anomsgtext
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=Issue321.astub Issue321.java
 */

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

interface Issue321 {
  @Nullable Optional<String> o();
}
