/*
 * @test
 * @summary A ReportUse with applyToSubpackages=false limits only its own annotation. An enclosing
 * package whose annotation applies to subpackages still reaches through it.
 *
 * @compile/fail/ref=ReportUseNested.out -XDrawDiagnostics -processor org.checkerframework.common.util.report.ReportChecker ru/package-info.java ru/sub/package-info.java ru/sub/deep/Deep.java
 */
public class ReportUseNested {}
