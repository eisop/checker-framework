package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Tests the conservative defaults for Initialization Checker and Nullness Checker. */
public class AnnotatedForNullnessTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * @param testFiles the files containing test code, which will be type-checked
     */
    public AnnotatedForNullnessTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AuseConservativeDefaultsForUncheckedCode=source,bytecode");
    }

    /**
     * This method returns the directories containing test code. Each directory will be type-checked
     * with {@code -AuseConservativeDefaultsForUncheckedCode=source,bytecode}.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {
            "nulless-conservative-defaults/annotatedfornullness",
            "nulless-conservative-defaults/packageannotatedfornullness"
        };
    }
}
