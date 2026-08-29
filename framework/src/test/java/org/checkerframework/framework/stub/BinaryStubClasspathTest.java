package org.checkerframework.framework.stub;

import org.junit.Assert;
import org.junit.Test;

/**
 * Guards against build-wiring rot for the generated {@code .astub.bin.gz} resources.
 *
 * <p>The binary stub files are produced by the {@code generateBinaryStubFiles} Gradle task and
 * wired into {@code processResources}/{@code sourcesJar} via explicit {@code dependsOn} hooks,
 * because the idiomatic {@code sourceSets.main.resources.srcDir(taskProvider)} wiring does not
 * register the producer dependency on this project's Gradle version: with that wiring, {@code
 * processResources} goes UP-TO-DATE without ever scheduling {@code generateBinaryStubFiles}, and
 * the built jars silently ship without any {@code .astub.bin.gz} resources. See commit 278a3e3b for
 * the revert and the evidence. If that {@code dependsOn} wiring ever rots the same way, the
 * binaries vanish from the classpath again, and {@link BinaryStubReader}/{@link
 * AnnotationFileElementTypes} silently fall back to text parsing with no diagnostic. This test
 * makes that failure loud by asserting that a known binary stub is present on the classpath, at the
 * exact resource path the private {@code parseOneStubFile} helper of {@link
 * AnnotationFileElementTypes#parseStubFiles} looks up.
 */
public class BinaryStubClasspathTest {

    /**
     * Asserts that the binary form of the framework's built-in {@code jdk11.astub} (parsed via
     * {@code parseOneStubFile(this.getClass(), ...)} in {@link
     * AnnotationFileElementTypes#parseStubFiles}, i.e. relative to {@link
     * AnnotationFileElementTypes}'s own package) is present on the classpath.
     */
    @Test
    public void frameworkBuiltinStubBinaryIsOnClasspath() {
        Assert.assertNotNull(
                "framework/src/main/java/org/checkerframework/framework/stub/jdk11.astub.bin.gz"
                        + " is missing from the classpath; generateBinaryStubFiles output is not"
                        + " wired into resources (see commit 278a3e3b)",
                AnnotationFileElementTypes.class.getResource(
                        "jdk11.astub" + BinaryStubData.BIN_SUFFIX));
    }
}
