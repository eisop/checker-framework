package org.checkerframework.checker.test.junit;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.jspecify.conformance.ConformanceTestRunner;
import org.jspecify.conformance.ReportedFact;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** An object to run the conformance tests against the EISOP Checker Framework. */
public final class NullnessJSpecifyConformanceTest {

    private final Path testDir;
    private final Path reportPath;
    private final ImmutableList<Path> deps;

    private static final ImmutableList<String> TEST_OPTIONS =
            ImmutableList.of(
                    "-AassumePure",
                    "-Adetailedmsgtext",
                    "-AcheckImpl",
                    "-AsuppressWarnings=conditional",
                    "-Astrict",
                    "-AshowTypes");

    public NullnessJSpecifyConformanceTest() {
        this.testDir = getSystemPropertyPath("ConformanceTest.inputs");
        this.reportPath = getSystemPropertyPath("ConformanceTest.report");
        this.deps =
                Splitter.on(":").splitToList(System.getProperty("ConformanceTest.deps")).stream()
                        .map(dep -> Paths.get(dep))
                        .collect(toImmutableList());
    }

    private Path getSystemPropertyPath(String propertyName) {
        String path = System.getProperty(propertyName);
        if (path == null) {
            throw new IllegalArgumentException("System property " + propertyName + " not set");
        }
        return Paths.get(path);
    }

    @Test
    public void conformanceTests() throws IOException {
        ConformanceTestRunner runner =
                new ConformanceTestRunner(NullnessJSpecifyConformanceTest::analyze);
        runner.checkConformance(testDir, deps, reportPath);
    }

    private static ImmutableSet<ReportedFact> analyze(
            Path testDir, ImmutableList<Path> files, ImmutableList<Path> deps) {
        ImmutableSet<File> fileInputs = files.stream().map(Path::toFile).collect(toImmutableSet());

        ImmutableList<String> depsAsStrings =
                deps.stream().map(Path::toString).collect(toImmutableList());

        TestConfiguration testConfig =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        null,
                        fileInputs,
                        depsAsStrings,
                        ImmutableList.of(NullnessChecker.class.getName()),
                        TEST_OPTIONS,
                        TestUtilities.getShouldEmitDebugInfo());
        TypecheckExecutor typecheckExecutor = new TypecheckExecutor();
        TypecheckResult result = typecheckExecutor.runTest(testConfig);
        ImmutableSet<ReportedFact> reportedFacts =
                result.getUnexpectedDiagnostics().stream()
                        .map(
                                diagnostic ->
                                        new FinalReportedFact(
                                                Path.of(diagnostic.getFilename()),
                                                diagnostic.getLineNumber(),
                                                diagnostic.getMessage()))
                        .collect(toImmutableSet());
        return reportedFacts;
    }
}

final class FinalReportedFact extends ReportedFact {
    private final String message;

    FinalReportedFact(Path filename, long lineNumber, String message) {
        super(filename, lineNumber);
        this.message = requireNonNull(message);
    }

    @Override
    protected boolean mustBeExpected() {
        return false;
    }

    @Override
    protected String getFactText() {
        return message;
    }
}
