package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Differential test for the binary stub format: verifies that loading the annotated JDK from {@code
 * annotated-jdk.bin.gz} and the built-in checker stub files from their {@code .astub.bin.gz} forms
 * (via {@code BinaryStubReader}) produces exactly the same annotations as parsing the text stubs
 * (via {@code AnnotationFileParser}), for every class in each binary stub.
 *
 * <p>The {@code -AbinaryStubDiffCheck} option makes {@code AnnotationFileElementTypes} run {@code
 * BinaryStubDiffChecker} at initialization, which reports each disagreement between the two paths
 * as an error; this test expects no diagnostics. A failure here means {@code BinaryStubWriter} or
 * {@code BinaryStubReader} diverges from the text parser's semantics for some construct in the
 * stubs — the binary path would silently drop or alter annotations for every user.
 *
 * <p>The Nullness Checker is used as the host checker because it exercises four annotated type
 * factories and the largest set of JDK annotations. The JDK check runs once per compilation;
 * built-in stub files are checked by each stub-types factory that loads them.
 */
public class NullnessBinaryStubDiffTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessBinaryStubDiffTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessBinaryStubDiffTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness-binarystubdiff",
                "-AbinaryStubDiffCheck");
    }

    /**
     * Returns the test directories.
     *
     * @return the test directories
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-binarystubdiff"};
    }
}
