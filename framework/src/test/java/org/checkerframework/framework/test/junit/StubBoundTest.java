package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.nontopdefault.StubBoundChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class StubBoundTest extends CheckerFrameworkPerDirectoryTest {

    public StubBoundTest(List<File> testFiles) {
        super(
                testFiles,
                StubBoundChecker.class,
                "stubbound",
                "-Astubs=tests/stubbound/StubBounds.astub",
                "-AmergeStubsWithSource");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stubbound"};
    }
}
