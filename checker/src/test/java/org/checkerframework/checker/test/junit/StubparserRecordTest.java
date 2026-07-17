package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

/**
 * Tests for stub parsing with records.
 *
 * <p>The records these stub files annotate are compiled from source, in this same directory, so the
 * test needs {@code -AmergeStubsWithSource}: that is what makes an annotation file apply to a class
 * that is being compiled.
 *
 * <p>Without it the test passed, but only by accident, and it was order-dependent. {@code
 * AnnotatedTypeFactory.fromElement} consults the stub files only when {@code
 * declarationFromElement} finds no tree for the element, so whether a source-declared element gets
 * its stub annotations depends on whether javac still has its tree at the moment of the element's
 * first use -- and whichever type is computed first is frozen into {@code elementCache} for the
 * rest of the compilation. Adding a file to this directory whose name sorts before {@code
 * PairRecord.java} was enough to change the answer: {@code new PairRecord(null)} in {@code
 * RecordUsage.java} began reporting {@code argument.type.incompatible}, because the constructor's
 * type was cached from its source declaration, without the {@code @Nullable} its stub file gives
 * it.
 */
public class StubparserRecordTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a StubparserRecordTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StubparserRecordTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "stubparser-records",
                "-Astubs=tests/stubparser-records",
                "-AmergeStubsWithSource");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        // Check for JDK 16+ without using a library:
        // There is no decimal point in the JDK 17 version number.
        if (System.getProperty("java.version").matches("^(1[6-9]|[2-9][0-9])(\\..*)?")) {
            return new String[] {"stubparser-records"};
        } else {
            return new String[] {};
        }
    }
}
