package org.checkerframework.framework.stubifier;

import org.checkerframework.framework.stub.BinaryStubBundle;
import org.checkerframework.framework.stub.BinaryStubData;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Tests for {@link BinaryStubFileGenerator}'s two modes (per-file siblings, and a whole-directory
 * {@code --bundle}), and for the source-fingerprint staleness detection that {@link
 * BinaryStubData#matchesSource} provides for a user-supplied {@code -Astubs} file (see {@code
 * AnnotationFileElementTypes#commandLineBinaryStub}, which relies on it instead of comparing file
 * modification times).
 *
 * <p>These tests call {@link BinaryStubFileGenerator#main} directly, so they exercise only its
 * success paths: its usage-error and bundle-name-collision paths call {@code System.exit}, which
 * would terminate the test JVM, and are intentionally not exercised here.
 */
public class BinaryStubFileGeneratorTest {

    /**
     * A fresh directory per test, deleted afterwards along with everything under it -- including a
     * bundle file, which {@code --bundle} writes as a sibling of the directory it covers rather
     * than inside it.
     */
    @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * A minimal, unannotated stub file body; annotations are irrelevant to what these tests check.
     */
    private static String stubSource(String className) {
        return "class "
                + className
                + " {\n  static Object m() { throw new RuntimeException(); }\n}\n";
    }

    /**
     * Writes {@code content} to {@code dir}/{@code name}, creating parent directories as needed.
     *
     * @param dir the directory to write into
     * @param name the file's name, possibly including subdirectories
     * @param content the file's content
     * @return the file's path
     * @throws IOException if the file cannot be written
     */
    private static Path writeStub(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /**
     * Per-file mode writes a sibling {@code .astub.bin.gz} whose recorded fingerprint matches the
     * exact source bytes it was generated from, and no other bytes.
     */
    @Test
    public void perFileModeWritesReadableSibling() throws Exception {
        Path dir = tempFolder.newFolder().toPath();
        Path stub = writeStub(dir, "Foo.astub", stubSource("Foo"));
        BinaryStubFileGenerator.main(new String[] {dir.toString(), dir.toString()});

        Path sibling = dir.resolve("Foo.astub" + BinaryStubWriter.BIN_SUFFIX);
        Assert.assertTrue("sibling binary must be written", Files.isRegularFile(sibling));

        BinaryStubData data;
        try (InputStream in = Files.newInputStream(sibling)) {
            data = new BinaryStubData(in);
        }
        Assert.assertTrue(data.classes.containsKey("Foo"));
        Assert.assertTrue(
                "the sibling's fingerprint must match the source it was generated from",
                data.matchesSource(Files.readAllBytes(stub)));
        Assert.assertFalse(
                "an edited source must no longer match the old fingerprint",
                data.matchesSource(
                        (stubSource("Foo") + "// edited\n").getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * {@code --bundle} mode combines every {@code .astub} file under the directory, including one
     * in a subdirectory, into a single file with one entry per source file, keyed by its
     * slash-separated relative path.
     */
    @Test
    public void bundleModeCombinesEveryFile() throws Exception {
        Path dir = tempFolder.newFolder().toPath();
        writeStub(dir, "Foo.astub", stubSource("Foo"));
        Path barStub = writeStub(dir, "sub/Bar.astub", stubSource("Bar"));

        BinaryStubFileGenerator.main(new String[] {"--bundle", dir.toString()});

        Path bundleFile = dir.resolveSibling(dir.getFileName() + BinaryStubWriter.BUNDLE_SUFFIX);
        Assert.assertTrue("bundle file must be written", Files.isRegularFile(bundleFile));

        BinaryStubBundle bundle;
        try (InputStream in = Files.newInputStream(bundleFile)) {
            bundle = new BinaryStubBundle(in);
        }

        BinaryStubData fooData = bundle.get("Foo.astub");
        Assert.assertNotNull("the bundle must have an entry for the top-level file", fooData);
        Assert.assertTrue(fooData.classes.containsKey("Foo"));

        BinaryStubData barData = bundle.get("sub/Bar.astub");
        Assert.assertNotNull("the bundle must have an entry for the nested file", barData);
        Assert.assertTrue(barData.classes.containsKey("Bar"));
        Assert.assertTrue(barData.matchesSource(Files.readAllBytes(barStub)));

        Assert.assertNull(
                "the bundle must have no entry for a file that was never in the directory",
                bundle.get("NoSuchFile.astub"));
    }

    /**
     * A {@code .jar} input root has each of its {@code .astub} entries pre-parsed into a new
     * sibling {@code .astub.bin.gz} entry, written into the same JAR file in place: the original
     * entries are untouched, and the output directory argument is ignored (there is nowhere else a
     * JAR's own entries could sensibly be written).
     */
    @Test
    public void jarModeWritesEntriesInPlace() throws Exception {
        Path dir = tempFolder.newFolder().toPath();
        Path jarPath = dir.resolve("stubs.jar");
        byte[] fooSource = stubSource("Foo").getBytes(StandardCharsets.UTF_8);
        writeJar(jarPath, "pkg/Foo.astub", fooSource);

        // The output directory is irrelevant for a JAR input root, but is still a required
        // positional argument.
        BinaryStubFileGenerator.main(
                new String[] {dir.resolve("unused-output").toString(), jarPath.toString()});

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Assert.assertNotNull(
                    "the original entry must be untouched", jarFile.getJarEntry("pkg/Foo.astub"));
            JarEntry binEntry = jarFile.getJarEntry("pkg/Foo.astub" + BinaryStubWriter.BIN_SUFFIX);
            Assert.assertNotNull("a sibling binary entry must have been added", binEntry);

            BinaryStubData data;
            try (InputStream in = jarFile.getInputStream(binEntry)) {
                data = new BinaryStubData(in);
            }
            Assert.assertTrue(data.classes.containsKey("Foo"));
            Assert.assertTrue(
                    "the entry's fingerprint must match the source it was generated from",
                    data.matchesSource(fooSource));
        }
    }

    /**
     * Regenerating a JAR whose entries already have binary siblings from a previous run replaces
     * them, rather than leaving stale duplicates.
     */
    @Test
    public void jarModeReplacesStaleEntry() throws Exception {
        Path dir = tempFolder.newFolder().toPath();
        Path jarPath = dir.resolve("stubs.jar");
        writeJar(jarPath, "Foo.astub", stubSource("Foo").getBytes(StandardCharsets.UTF_8));
        BinaryStubFileGenerator.main(
                new String[] {dir.resolve("unused-output").toString(), jarPath.toString()});

        // Edit the entry (by rewriting the whole JAR, since JarOutputStream cannot append to
        // an existing one) and regenerate: the sibling entry from the first run must be
        // replaced, not duplicated or left stale.
        byte[] editedSource = (stubSource("Foo") + "// edited\n").getBytes(StandardCharsets.UTF_8);
        try (JarFile oldJar = new JarFile(jarPath.toFile())) {
            JarEntry binEntry = oldJar.getJarEntry("Foo.astub" + BinaryStubWriter.BIN_SUFFIX);
            Assert.assertNotNull(binEntry);
        }
        writeJar(jarPath, "Foo.astub", editedSource);
        BinaryStubFileGenerator.main(
                new String[] {dir.resolve("unused-output").toString(), jarPath.toString()});

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry binEntry = jarFile.getJarEntry("Foo.astub" + BinaryStubWriter.BIN_SUFFIX);
            Assert.assertNotNull(binEntry);
            BinaryStubData data;
            try (InputStream in = jarFile.getInputStream(binEntry)) {
                data = new BinaryStubData(in);
            }
            Assert.assertTrue(
                    "the replaced entry's fingerprint must match the edited source",
                    data.matchesSource(editedSource));

            int matchingEntries = 0;
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                if (entries.nextElement()
                        .getName()
                        .equals("Foo.astub" + BinaryStubWriter.BIN_SUFFIX)) {
                    matchingEntries++;
                }
            }
            Assert.assertEquals(
                    "the JAR must have exactly one entry with this name, not a stale duplicate",
                    1,
                    matchingEntries);
        }
    }

    /**
     * When a {@code .astub} entry that already has a binary sibling is edited into something
     * unparseable, regenerating removes the now-unregenerable sibling instead of leaving it behind:
     * unlike the directory case (which calls {@code deleteStaleOutput}) or a bundle (which is
     * rewritten from scratch each time), a JAR is modified incrementally, so this must be handled
     * explicitly -- otherwise the stale sibling would trigger a {@code stale.binary.stub} warning
     * on every future compilation, with the tool's own suggested fix (re-running it) unable to
     * clear it.
     */
    @Test
    public void jarModeRemovesEntryThatNoLongerParses() throws Exception {
        Path dir = tempFolder.newFolder().toPath();
        Path jarPath = dir.resolve("stubs.jar");
        writeJar(jarPath, "Foo.astub", stubSource("Foo").getBytes(StandardCharsets.UTF_8));
        BinaryStubFileGenerator.main(
                new String[] {dir.resolve("unused-output").toString(), jarPath.toString()});
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Assert.assertNotNull(
                    "sibling must exist after the first, successful run",
                    jarFile.getJarEntry("Foo.astub" + BinaryStubWriter.BIN_SUFFIX));
        }

        // Rewrite the JAR with an unparseable entry (JarOutputStream cannot edit a single
        // entry in place) and regenerate.
        writeJar(
                jarPath, "Foo.astub", "not valid java at all {{{".getBytes(StandardCharsets.UTF_8));
        BinaryStubFileGenerator.main(
                new String[] {dir.resolve("unused-output").toString(), jarPath.toString()});

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Assert.assertNull(
                    "the now-stale sibling must have been removed, not left behind",
                    jarFile.getJarEntry("Foo.astub" + BinaryStubWriter.BIN_SUFFIX));
            Assert.assertNotNull(
                    "the (unparseable) source entry itself must be untouched",
                    jarFile.getJarEntry("Foo.astub"));
        }
    }

    /**
     * A directory passed to per-file mode is searched recursively for {@code .jar} files, not just
     * {@code .astub} files, and each one found is updated the same way a directly-named {@code
     * .jar} root would be -- matching {@code AnnotationFileUtil}'s directory walk, which likewise
     * descends into a nested {@code .jar} when resolving {@code -Astubs}. Without this, a nested
     * jar's entries could never get a binary form through the enclosing directory root, permanently
     * triggering the "incomplete binary stub setup" warning with no way to clear it.
     */
    @Test
    public void directoryModeRecursesIntoNestedJar() throws Exception {
        Path dir = tempFolder.newFolder().toPath();
        writeStub(dir, "Foo.astub", stubSource("Foo"));
        Path nestedJar = dir.resolve("sub/nested.jar");
        Files.createDirectories(nestedJar.getParent());
        writeJar(nestedJar, "Bar.astub", stubSource("Bar").getBytes(StandardCharsets.UTF_8));

        BinaryStubFileGenerator.main(new String[] {dir.toString(), dir.toString()});

        Assert.assertTrue(
                "the loose file's sibling must still be written",
                Files.isRegularFile(dir.resolve("Foo.astub" + BinaryStubWriter.BIN_SUFFIX)));
        try (JarFile jarFile = new JarFile(nestedJar.toFile())) {
            Assert.assertNotNull(
                    "the nested jar's entry must have gotten a sibling too",
                    jarFile.getJarEntry("Bar.astub" + BinaryStubWriter.BIN_SUFFIX));
        }
    }

    /**
     * Writes a JAR file at {@code jarPath} (overwriting one if present) with a single entry.
     *
     * @param jarPath the path to write the JAR to
     * @param entryName the single entry's name
     * @param entryContent the single entry's content
     * @throws IOException if the JAR cannot be written
     */
    private static void writeJar(Path jarPath, String entryName, byte[] entryContent)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jarOut = new JarOutputStream(bytes)) {
            jarOut.putNextEntry(new JarEntry(entryName));
            jarOut.write(entryContent);
            jarOut.closeEntry();
        }
        Files.write(jarPath, bytes.toByteArray());
    }

    /**
     * A bare relative input root with no directory component (e.g. {@code "project-stubs"}, the
     * exact form the manual's own {@code --bundle} example uses) must not crash: {@code
     * Path#getParent()} is {@code null} for such a path, which {@code Files.createTempFile} rejects
     * with a {@code NullPointerException} unless the path is first made absolute. {@link
     * BinaryStubFileGenerator#main} resolves relative arguments against the process's current
     * working directory, which cannot be changed within this JVM once started, so this runs the
     * generator in a genuinely separate process with a controlled working directory, passing bare
     * relative names exactly as a user would from within that directory.
     */
    @Test
    public void bareRelativeBundleRootDoesNotCrash() throws Exception {
        Path dir = tempFolder.newFolder().toPath();
        Path stubDir = dir.resolve("project-stubs");
        writeStub(stubDir, "Foo.astub", stubSource("Foo"));

        runGeneratorInSubprocess(dir, "--bundle", "project-stubs");

        Path bundleFile = dir.resolve("project-stubs" + BinaryStubWriter.BUNDLE_SUFFIX);
        Assert.assertTrue(
                "bundle file must be written despite the bare relative argument",
                Files.isRegularFile(bundleFile));
    }

    /**
     * The JAR-mode analogue of {@link #bareRelativeBundleRootDoesNotCrash}: a bare relative {@code
     * .jar} argument (e.g. {@code "project-stubs.jar"}, the manual's own example) must not crash
     * either.
     */
    @Test
    public void bareRelativeJarRootDoesNotCrash() throws Exception {
        Path dir = tempFolder.newFolder().toPath();
        writeJar(
                dir.resolve("project-stubs.jar"),
                "Foo.astub",
                stubSource("Foo").getBytes(StandardCharsets.UTF_8));

        runGeneratorInSubprocess(dir, "project-stubs.jar", "project-stubs.jar");

        try (JarFile jarFile = new JarFile(dir.resolve("project-stubs.jar").toFile())) {
            Assert.assertNotNull(
                    "a sibling binary entry must have been added despite the bare relative"
                            + " argument",
                    jarFile.getJarEntry("Foo.astub" + BinaryStubWriter.BIN_SUFFIX));
        }
    }

    /**
     * Runs {@link BinaryStubFileGenerator#main} with {@code args} in a separate JVM process whose
     * working directory is {@code workingDir}, on the current process's own classpath (this test
     * class, and therefore {@link BinaryStubFileGenerator}, is necessarily already on it).
     *
     * @param workingDir the subprocess's working directory
     * @param args the generator's command-line arguments
     * @throws Exception if the subprocess cannot be started, or exits with a non-zero status
     */
    private static void runGeneratorInSubprocess(Path workingDir, String... args) throws Exception {
        String javaBin =
                System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(BinaryStubFileGenerator.class.getName());
        command.addAll(Arrays.asList(args));

        Process process =
                new ProcessBuilder(command)
                        .directory(workingDir.toFile())
                        .redirectErrorStream(true)
                        .start();
        String output = new String(readAllBytes(process.getInputStream()), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        Assert.assertEquals(
                "generator subprocess must not crash; output was:\n" + output, 0, exitCode);
    }

    /**
     * Reads all of {@code in}'s remaining bytes.
     *
     * @param in the stream to read
     * @return the bytes read
     * @throws IOException if reading fails
     */
    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}
