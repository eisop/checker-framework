package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.immutability.PICOChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;

public class ImmutabilityTypecheckTest extends CheckerFrameworkPerFileTest {
    public ImmutabilityTypecheckTest(File testFile) {
        super(testFile, PICOChecker.class, "immutability", "-Anomsgtext", "-Anocheckjdk");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"immutability"};
    }
}
