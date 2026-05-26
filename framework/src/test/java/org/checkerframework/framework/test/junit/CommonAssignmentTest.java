package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.commonassignment.CommonAssignmentChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Regression tests for the contract of {@code BaseTypeVisitor.commonAssignmentCheck(Tree,
 * ExpressionTree, ...)}. See PR #736: the method must return {@code false} when {@code
 * validateType} fails for the LHS of an assignment.
 */
public class CommonAssignmentTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Creates a {@link CommonAssignmentTest}.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public CommonAssignmentTest(List<File> testFiles) {
        super(testFiles, CommonAssignmentChecker.class, "commonassignment");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"commonassignment"};
    }
}
