// package org.checkerframework.checker.test.junit;
//
// import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
// import org.junit.runners.Parameterized;
//
// import java.io.File;
// import java.util.List;
//
/// ** Glacier tests in PICO. */
// public class PICOGlacierTest extends CheckerFrameworkPerDirectoryTest {
//    /**
//     * Create a PICOGlacierTest.
//     *
//     * @param testFiles the files containing test code, which will be type-checked
//     */
//    public PICOGlacierTest(List<File> testFiles) {
//        super(
//                testFiles,
//                org.checkerframework.checker.pico.PICOChecker.class,
//                "pico-glacier",
//                "-AassumeInitialized");
//    }
//
//    /**
//     * Returns the test files for the PICO Checker. The test files are in the "tests/pico-glacier"
//     * directory.
//     */
//    @Parameterized.Parameters
//    public static String[] getTestDirs() {
//        return new String[] {"pico-glacier"};
//    }
// }
