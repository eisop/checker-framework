/*
 * @test
 * @summary Test that a checker's default SuppressWarnings prefix also suppresses findings from
 * checkers it runs as a (possibly indirect) subchecker, per SourceChecker#getUpstreamCheckerNames.
 * -ArequirePrefixInWarningSuppressions rules out the unrelated partial-message-key suppression
 * fallback, so each of these can only pass because of that upstream-derived prefix.
 *
 * @compile/fail/ref=UpstreamPrefixIndex.out -XDrawDiagnostics -ArequirePrefixInWarningSuppressions -processor org.checkerframework.checker.index.IndexChecker UpstreamPrefixIndex.java
 * @compile/ref=UpstreamPrefixNonEmpty.out -XDrawDiagnostics -ArequirePrefixInWarningSuppressions -processor org.checkerframework.checker.optional.OptionalChecker UpstreamPrefixNonEmpty.java
 */

public class Main {}
