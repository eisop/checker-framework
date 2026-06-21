package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.interning.InterningChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * JUnit test for the configurable Java 8 type-argument-inference work budget. Runs with a small
 * {@code -AinferenceWorkBudget} so a shallow nested generic invocation triggers the budget, instead
 * of needing a 100+-deep chain at the default budget. The budget is a framework mechanism; the
 * Interning Checker is used only because some standard checker is required.
 */
public class InferenceBudgetTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create an InferenceBudgetTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public InferenceBudgetTest(List<File> testFiles) {
        super(testFiles, InterningChecker.class, "interning", "-AinferenceWorkBudget=2000");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"inference-budget"};
    }
}
