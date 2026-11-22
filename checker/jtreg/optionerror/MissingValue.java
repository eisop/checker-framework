/*
 * @test
 *
 * @summary Test that an option that a type factory reads, given without a value, is reported as
 * a compiler error instead of crashing the type factory's constructor with a NullPointerException.
 *
 * @compile/fail/ref=MissingAliasedTypeAnnos.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AaliasedTypeAnnos MissingValue.java
 * @compile/fail/ref=MissingAliasedDeclAnnos.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AaliasedDeclAnnos MissingValue.java
 * @compile/fail/ref=MissingFlowdotdir.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -Aflowdotdir MissingValue.java
 */

public class MissingValue {}
