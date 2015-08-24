package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class FenumSwingTest extends DefaultCheckerTest {

    public FenumSwingTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.fenum.FenumChecker.class,
                "fenum",
                "-Anomsgtext",
                "-Aquals=org.checkerframework.checker.fenum.qual.SwingVerticalOrientation,org.checkerframework.checker.fenum.qual.SwingHorizontalOrientation,org.checkerframework.checker.fenum.qual.SwingBoxOrientation,org.checkerframework.checker.fenum.qual.SwingCompassDirection,org.checkerframework.checker.fenum.qual.SwingElementOrientation,org.checkerframework.checker.fenum.qual.SwingTextOrientation");
        // TODO: check all qualifiers
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"fenumswing", "all-systems"};
    }
}