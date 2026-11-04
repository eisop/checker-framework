package org.checkerframework.framework.test.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * Tests the ajavaChecks option configuration and behavior in {@link
 * CheckerFrameworkPerDirectoryTest}.
 */
public class AjavaChecksOptionTest {

    /**
     * An abstract test harness extending {@link CheckerFrameworkPerDirectoryTest} to inspect
     * options. Being abstract, it is not executed as a test suite.
     */
    private abstract static class DummyHarness extends CheckerFrameworkPerDirectoryTest {
        /** Creates a dummy harness. */
        DummyHarness() {
            super(
                    Collections.<java.io.File>emptyList(),
                    Collections.<String>emptyList(),
                    "",
                    Collections.<String>emptyList());
        }

        /**
         * Returns the checker options passed to this test.
         *
         * @return the checker options
         */
        List<String> getCheckerOptions() {
            return checkerOptions;
        }
    }

    /** Tests that -AajavaChecks is included when the ajavaChecks system property is set to true. */
    @Test
    public void testAjavaChecksOptionWhenEnabled() {
        String prev = System.getProperty("ajavaChecks");
        try {
            System.setProperty("ajavaChecks", "true");
            Assert.assertTrue(TestUtilities.getShouldRunAjavaChecks());
            DummyHarness harness = new DummyHarness() {};
            Assert.assertTrue(harness.getCheckerOptions().contains("-AajavaChecks"));
        } finally {
            if (prev != null) {
                System.setProperty("ajavaChecks", prev);
            } else {
                System.clearProperty("ajavaChecks");
            }
        }
    }

    /** Tests that -AajavaChecks is omitted when the ajavaChecks system property is unset. */
    @Test
    public void testAjavaChecksOptionWhenUnset() {
        String prev = System.getProperty("ajavaChecks");
        try {
            System.clearProperty("ajavaChecks");
            Assert.assertFalse(TestUtilities.getShouldRunAjavaChecks());
            DummyHarness harness = new DummyHarness() {};
            Assert.assertFalse(harness.getCheckerOptions().contains("-AajavaChecks"));
        } finally {
            if (prev != null) {
                System.setProperty("ajavaChecks", prev);
            } else {
                System.clearProperty("ajavaChecks");
            }
        }
    }

    /** Tests that -AajavaChecks is omitted when the ajavaChecks system property is set to false. */
    @Test
    public void testAjavaChecksOptionWhenExplicitlyFalse() {
        String prev = System.getProperty("ajavaChecks");
        try {
            System.setProperty("ajavaChecks", "false");
            Assert.assertFalse(TestUtilities.getShouldRunAjavaChecks());
            DummyHarness harness = new DummyHarness() {};
            Assert.assertFalse(harness.getCheckerOptions().contains("-AajavaChecks"));
        } finally {
            if (prev != null) {
                System.setProperty("ajavaChecks", prev);
            } else {
                System.clearProperty("ajavaChecks");
            }
        }
    }

    /**
     * Tests that CheckerFrameworkPerDirectoryTest option configuration matches the active
     * environment.
     */
    @Test
    public void testCurrentEnvironmentConfiguration() {
        boolean expected = TestUtilities.getShouldRunAjavaChecks();
        DummyHarness harness = new DummyHarness() {};
        Assert.assertEquals(
                "CheckerFrameworkPerDirectoryTest should match TestUtilities.getShouldRunAjavaChecks()",
                expected,
                harness.getCheckerOptions().contains("-AajavaChecks"));
    }
}
