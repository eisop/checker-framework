package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.stub.BinaryStubData;
import org.junit.Assert;
import org.junit.Test;

/**
 * Guards against build-wiring rot for the generated {@code .astub.bin.gz} resources, for a checker
 * built-in stub (the framework-level counterpart is {@code
 * org.checkerframework.framework.stub.BinaryStubClasspathTest} in the {@code framework}
 * subproject).
 *
 * <p>The binary stub files are produced by the {@code generateBinaryStubFiles} Gradle task and
 * wired into {@code processResources}/{@code sourcesJar} via explicit {@code dependsOn} hooks,
 * because the idiomatic {@code sourceSets.main.resources.srcDir(taskProvider)} wiring does not
 * register the producer dependency on this project's Gradle version: with that wiring, {@code
 * processResources} goes UP-TO-DATE without ever scheduling {@code generateBinaryStubFiles}, and
 * the built jars silently ship without any {@code .astub.bin.gz} resources. See commit 278a3e3b for
 * the revert and the evidence. If that {@code dependsOn} wiring ever rots the same way, the
 * binaries vanish from the classpath again, and the reader silently falls back to text parsing with
 * no diagnostic. This test makes that failure loud by asserting that a known checker built-in
 * binary stub is present on the classpath, at the exact resource path the framework's {@code
 * AnnotationFileElementTypes.parseOneStubFile} looks up (relative to the checker class' own
 * package, since {@code parseOneStubFile} is also called with {@code checker.getClass()}).
 */
public class BinaryStubClasspathTest {

    /**
     * Asserts that the binary form of the nullness checker's built-in {@code jdk11.astub} is
     * present on the classpath.
     */
    @Test
    public void nullnessBuiltinStubBinaryIsOnClasspath() {
        Assert.assertNotNull(
                "checker/src/main/java/org/checkerframework/checker/nullness/jdk11.astub.bin.gz"
                        + " is missing from the classpath; generateBinaryStubFiles output is not"
                        + " wired into resources (see commit 278a3e3b)",
                NullnessChecker.class.getResource("jdk11.astub" + BinaryStubData.BIN_SUFFIX));
    }
}
