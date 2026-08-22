package org.checkerframework.framework.stub;

import org.checkerframework.framework.stubifier.BinaryStubFileGenerator;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * End-to-end tests for the reader side of binary {@code -Astubs} support: {@link
 * AnnotationFileElementTypes}'s {@code parseCommandLineStubLocation}, {@code
 * parseCommandLineStubResource}, {@code commandLineBinaryStub}, and the {@code
 * useBinaryForCommandLineStubs} gate. {@code
 * org.checkerframework.framework.stubifier.BinaryStubFileGeneratorTest} covers only the writer
 * (round-tripping per-file and bundle output); this class drives real compilations through {@link
 * TypecheckExecutor}, the same harness {@code CheckerFrameworkPerDirectoryTest} uses, so it
 * exercises the reader with dynamically generated fixtures that a checked-in jtreg test cannot have
 * (a checked-in {@code .astub.bin.gz} would defeat the point of a content digest).
 *
 * <p>Uses the {@code SubtypingChecker} with the test-only {@code Encrypted} qualifier ({@code
 * org.checkerframework.framework.testchecker.util.Encrypted}) rather than a real checker like
 * Nullness, so this test stays entirely within the {@code framework} module (Nullness lives in
 * {@code checker}, which depends on {@code framework}, not the other way around). A stub-annotated
 * {@code Lib.maybeEncrypted()} returns either {@code @Encrypted Object} (if its stub's annotation
 * took effect) or the default, unqualified {@code Object} (if it did not); passing the latter where
 * {@code @Encrypted} is required is an {@code argument.type.incompatible} error, so that error's
 * presence or absence is a direct, unambiguous signal of whether the stub was applied at all --
 * from either its text or its binary form.
 */
public class BinaryStubsCommandLineTest {

    /** The source of the real class a stub annotates, compiled together with the test source. */
    private static final String LIB_SOURCE =
            "public class Lib {\n"
                    + "    public static Object maybeEncrypted() { return null; }\n"
                    + "}\n";

    /** The source of the class under test, which calls the stub-annotated method. */
    private static final String USE_SOURCE =
            "import org.checkerframework.framework.testchecker.util.Encrypted;\n"
                    + "class Use {\n"
                    + "    static void needsEncrypted(@Encrypted Object o) {}\n"
                    + "    void m() {\n"
                    + "        needsEncrypted(Lib.maybeEncrypted());\n"
                    + "    }\n"
                    + "}\n";

    /** A stub annotating {@code Lib.maybeEncrypted()}'s return type as {@code @Encrypted}. */
    private static final String LIB_STUB_ENCRYPTED =
            "import org.checkerframework.framework.testchecker.util.Encrypted;\n"
                    + "public class Lib {\n"
                    + "    public static @Encrypted Object maybeEncrypted();\n"
                    + "}\n";

    /** A stub for {@code Lib.maybeEncrypted()} that does not annotate its return type at all. */
    private static final String LIB_STUB_UNANNOTATED =
            "public class Lib {\n" + "    public static Object maybeEncrypted();\n" + "}\n";

    /** A trivial, unrelated stub file, used to make a directory's binary setup incomplete. */
    private static final String OTHER_STUB = "class Other {\n}\n";

    /**
     * Message key reported when {@code @Encrypted} was not applied to {@code Lib.maybeEncrypted()}.
     */
    private static final String MISMATCH_KEY = "argument.type.incompatible";

    /** Message key for a stale {@code -Astubs} binary stub. */
    private static final String STALE_KEY = "stale.binary.stub";

    /** Message key for a {@code -Astubs} directory with an incomplete binary stub setup. */
    private static final String INCOMPLETE_SETUP_KEY = "text.parsing.command.line.stub";

    /** A fresh temporary directory, created before and deleted after each test. */
    private Path tempDir;

    /**
     * Creates {@link #tempDir}.
     *
     * @throws IOException if the directory cannot be created
     */
    @Before
    public void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("binarystubscommandlinetest");
    }

    /**
     * Deletes {@link #tempDir} and everything under it.
     *
     * @throws IOException never (failures are reported to stderr, not thrown), so a cleanup failure
     *     does not mask a test's own assertion failure
     */
    @After
    public void deleteTempDir() throws IOException {
        deleteRecursively(tempDir.toFile());
    }

    /**
     * Writes {@code content} to {@code path}, creating parent directories as needed.
     *
     * @param path the file to write
     * @param content the file's content
     * @throws IOException if the file cannot be written
     */
    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Compiles fresh copies of {@link #LIB_SOURCE} and {@link #USE_SOURCE} with the Subtyping
     * Checker and the given extra options, and returns the actual diagnostics.
     *
     * @param extraOptions options in addition to {@code -Aquals=...} (e.g. {@code -Astubs=...})
     * @return the diagnostics the compilation produced
     * @throws IOException if writing the source files fails
     */
    private List<Diagnostic<? extends JavaFileObject>> compile(String... extraOptions)
            throws IOException {
        // Lib.java is compiled separately, ahead of time, and put on the classpath -- not
        // compiled as a source file alongside Use.java. A -Astubs file is meant to annotate a
        // library whose source is not part of the current compilation; a class that IS being
        // compiled from source in the same batch uses its own (unannotated) declaration, and the
        // checker never even consults a stub for it. This matters for these tests specifically
        // because they must distinguish "the stub's annotation took effect" from "the checker
        // fell back to Lib's real, unannotated declaration" -- which looks identical unless Lib
        // is genuinely precompiled, exactly as a real library would be.
        File libClasses = compileLib();

        File useFile = tempDir.resolve("Use.java").toFile();
        write(useFile.toPath(), USE_SOURCE);

        List<String> options = new ArrayList<>();
        options.add(
                "-Aquals=org.checkerframework.framework.testchecker.util.Encrypted,"
                        + "org.checkerframework.framework.testchecker.util.PolyEncrypted,"
                        + "org.checkerframework.common.subtyping.qual.Unqualified");
        options.addAll(Arrays.asList(extraOptions));

        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        tempDir.toString(),
                        Collections.singletonList(useFile),
                        Collections.singletonList(libClasses.getAbsolutePath()),
                        Collections.singletonList(
                                "org.checkerframework.common.subtyping.SubtypingChecker"),
                        options,
                        false);
        TypecheckResult result = new TypecheckExecutor().runTest(config);
        return result.getActualDiagnostics();
    }

    /**
     * Compiles a fresh copy of {@link #LIB_SOURCE} (plain {@code javac}, no annotation processing)
     * into a classes directory under {@link #tempDir}, and returns that directory.
     *
     * @return the directory {@code Lib.class} was compiled into
     * @throws IOException if writing the source or compiling it fails
     */
    private File compileLib() throws IOException {
        File libFile = tempDir.resolve("lib-src").resolve("Lib.java").toFile();
        write(libFile.toPath(), LIB_SOURCE);
        File libClasses = tempDir.resolve("lib-classes").toFile();
        Assert.assertTrue(libClasses.mkdirs());
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result =
                compiler.run(
                        null,
                        null,
                        null,
                        "-d",
                        libClasses.getAbsolutePath(),
                        "-proc:none",
                        libFile.getAbsolutePath());
        Assert.assertEquals("compiling Lib.java itself must succeed", 0, result);
        return libClasses;
    }

    /**
     * Returns true if {@code diagnostics} contains one whose message is exactly the parenthesized
     * message key (the format {@code -Anomsgtext}, which {@link TestConfigurationBuilder} always
     * adds, produces).
     *
     * @param diagnostics the diagnostics to search
     * @param key the message key to look for
     * @return true if found
     */
    private static boolean has(List<Diagnostic<? extends JavaFileObject>> diagnostics, String key) {
        String expected = "(" + key + ")";
        for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
            if (expected.equals(d.getMessage(null))) {
                return true;
            }
        }
        return false;
    }

    /** Sanity check: with no {@code -Astubs} at all, the mismatch is reported. */
    @Test
    public void noStubGivesMismatch() throws IOException {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile();
        Assert.assertTrue(
                "expected " + MISMATCH_KEY + " with no stub file at all",
                has(diagnostics, MISMATCH_KEY));
    }

    /** A fresh per-file binary sibling is read and applied, with no extra warnings. */
    @Test
    public void freshPerFileBinaryIsApplied() throws IOException {
        Path stubsDir = tempDir.resolve("stubs");
        write(stubsDir.resolve("Lib.astub"), LIB_STUB_ENCRYPTED);
        BinaryStubFileGenerator.main(new String[] {stubsDir.toString(), stubsDir.toString()});
        Assert.assertTrue(
                "generator must have written a sibling binary",
                Files.isRegularFile(stubsDir.resolve("Lib.astub.bin.gz")));

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + stubsDir);
        Assert.assertFalse(
                "the binary stub's @Encrypted must have been applied",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertFalse(
                "a fresh, complete binary setup must not warn", has(diagnostics, STALE_KEY));
        Assert.assertFalse(
                "a fresh, complete binary setup must not warn",
                has(diagnostics, INCOMPLETE_SETUP_KEY));
    }

    /**
     * A per-file binary sibling that no longer matches its {@code .astub} file's content (the file
     * was edited after the binary was generated) is detected as stale, falls back to text-parsing
     * the edited content, and warns -- never silently applies the stale annotation.
     */
    @Test
    public void staleBinaryFallsBackToEditedText() throws IOException {
        Path stubsDir = tempDir.resolve("stubs");
        write(stubsDir.resolve("Lib.astub"), LIB_STUB_ENCRYPTED);
        BinaryStubFileGenerator.main(new String[] {stubsDir.toString(), stubsDir.toString()});
        // Edit the .astub file after generating its binary, without regenerating: the binary is
        // now stale.
        write(stubsDir.resolve("Lib.astub"), LIB_STUB_UNANNOTATED);

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + stubsDir);
        Assert.assertTrue(
                "a stale binary must fall back to the edited (unannotated) text, not the stale"
                        + " @Encrypted binary",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertTrue("a stale binary must be warned about", has(diagnostics, STALE_KEY));
    }

    /** A fresh {@code --bundle} covering the whole directory is read and applied. */
    @Test
    public void freshBundleIsApplied() throws IOException {
        Path stubsDir = tempDir.resolve("stubs");
        write(stubsDir.resolve("Lib.astub"), LIB_STUB_ENCRYPTED);
        BinaryStubFileGenerator.main(new String[] {"--bundle", stubsDir.toString()});
        Assert.assertTrue(
                "generator must have written a bundle",
                Files.isRegularFile(tempDir.resolve("stubs.astub.bin.gz")));

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + stubsDir);
        Assert.assertFalse(
                "the bundle's @Encrypted must have been applied", has(diagnostics, MISMATCH_KEY));
        Assert.assertFalse("a fresh, complete bundle must not warn", has(diagnostics, STALE_KEY));
        Assert.assertFalse(
                "a fresh, complete bundle must not warn", has(diagnostics, INCOMPLETE_SETUP_KEY));
    }

    /**
     * A directory with a bundle for only some of its {@code .astub} files (a file was added after
     * the bundle was generated) still applies the bundled file's binary form, text-parses the
     * unbundled file, and warns about the incomplete setup.
     */
    @Test
    public void incompleteBundleSetupWarns() throws IOException {
        Path stubsDir = tempDir.resolve("stubs");
        write(stubsDir.resolve("Lib.astub"), LIB_STUB_ENCRYPTED);
        BinaryStubFileGenerator.main(new String[] {"--bundle", stubsDir.toString()});
        // Added after the bundle was generated, so it has no entry in the bundle.
        write(stubsDir.resolve("Other.astub"), OTHER_STUB);

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + stubsDir);
        Assert.assertFalse(
                "the bundled file's @Encrypted must still have been applied",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertTrue(
                "a directory with only some files covered by a binary form must warn",
                has(diagnostics, INCOMPLETE_SETUP_KEY));
    }

    /**
     * {@code -AstubWarnIfNotFound} forces text parsing even when a fresh binary is available (the
     * gate in {@code useBinaryForCommandLineStubs}), so the diagnostics are identical to text
     * parsing and neither binary-specific warning is ever reached.
     */
    @Test
    public void stubWarnIfNotFoundGateForcesTextParsing() throws IOException {
        Path stubsDir = tempDir.resolve("stubs");
        write(stubsDir.resolve("Lib.astub"), LIB_STUB_ENCRYPTED);
        BinaryStubFileGenerator.main(new String[] {stubsDir.toString(), stubsDir.toString()});

        List<Diagnostic<? extends JavaFileObject>> diagnostics =
                compile("-Astubs=" + stubsDir, "-AstubWarnIfNotFound");
        Assert.assertFalse(
                "text-parsing the stub must apply @Encrypted just as the binary form would",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertFalse(
                "the gate must bypass the binary path entirely, so it never issues either of its"
                        + " own warnings",
                has(diagnostics, STALE_KEY));
        Assert.assertFalse(
                "the gate must bypass the binary path entirely, so it never issues either of its"
                        + " own warnings",
                has(diagnostics, INCOMPLETE_SETUP_KEY));
    }

    /**
     * A per-file binary sibling for a {@code .astub} file nested in a subdirectory of the {@code
     * -Astubs} directory is found and applied, exercising the relative-path computation that a flat
     * (single-level) {@code -Astubs} directory does not.
     */
    @Test
    public void nestedPerFileBinaryIsApplied() throws IOException {
        Path stubsDir = tempDir.resolve("stubs");
        write(stubsDir.resolve("sub/Lib.astub"), LIB_STUB_ENCRYPTED);
        BinaryStubFileGenerator.main(new String[] {stubsDir.toString(), stubsDir.toString()});
        Assert.assertTrue(
                "generator must have written a sibling binary for the nested file",
                Files.isRegularFile(stubsDir.resolve("sub/Lib.astub.bin.gz")));

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + stubsDir);
        Assert.assertFalse(
                "a nested file's per-file sibling must have been applied",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertFalse(has(diagnostics, STALE_KEY));
        Assert.assertFalse(has(diagnostics, INCOMPLETE_SETUP_KEY));
    }

    /**
     * A bundle entry for a {@code .astub} file nested in a subdirectory of the bundled directory is
     * found (by its slash-separated relative path) and applied.
     */
    @Test
    public void nestedBundleEntryIsApplied() throws IOException {
        Path stubsDir = tempDir.resolve("stubs");
        write(stubsDir.resolve("sub/Lib.astub"), LIB_STUB_ENCRYPTED);
        BinaryStubFileGenerator.main(new String[] {"--bundle", stubsDir.toString()});
        Assert.assertTrue(
                "generator must have written a bundle",
                Files.isRegularFile(tempDir.resolve("stubs.astub.bin.gz")));

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + stubsDir);
        Assert.assertFalse(
                "a nested file's bundle entry must have been applied",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertFalse(has(diagnostics, STALE_KEY));
        Assert.assertFalse(has(diagnostics, INCOMPLETE_SETUP_KEY));
    }

    /**
     * A directory {@code stubs} passed to {@code -Astubs} can coincidentally share its bundle's
     * target name ({@code stubs.astub.bin.gz}, beside it) with an unrelated top-level file {@code
     * stubs.astub}'s own per-file sibling (also named {@code stubs.astub.bin.gz}, since {@code
     * stubs.astub}'s sibling-naming rule appends the same suffix to a name that already ends in
     * {@code .astub}). {@code binaryStubBundleFor} must recognize, via the magic number, that the
     * file at that path is not actually a bundle, and silently fall back -- without misapplying
     * anything and without a spurious diagnostic -- to per-file siblings, which still work normally
     * for files genuinely inside {@code stubs/}.
     */
    @Test
    public void bundleNameCollisionWithPerFileSiblingIsIgnored() throws IOException {
        Path stubsDir = tempDir.resolve("stubs");
        write(stubsDir.resolve("Lib.astub"), LIB_STUB_ENCRYPTED);
        // Unrelated top-level file whose own per-file sibling, "stubs.astub.bin.gz", lands at
        // exactly the path where a bundle for the "stubs" directory would be looked up.
        write(tempDir.resolve("stubs.astub"), OTHER_STUB);

        BinaryStubFileGenerator.main(new String[] {tempDir.toString(), tempDir.toString()});
        Path collisionPath = tempDir.resolve("stubs.astub.bin.gz");
        Assert.assertTrue(
                "the unrelated file's own sibling must exist at the colliding path",
                Files.isRegularFile(collisionPath));
        Assert.assertFalse(
                "the colliding file must be an ordinary per-file binary, not a bundle, for this"
                        + " test to exercise the collision",
                looksLikeBundleMagic(collisionPath));
        Assert.assertTrue(
                "Lib.astub's own sibling must also have been written",
                Files.isRegularFile(stubsDir.resolve("Lib.astub.bin.gz")));

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + stubsDir);
        Assert.assertFalse(
                "Lib's own per-file sibling must still be found and applied despite the collision",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertFalse(has(diagnostics, STALE_KEY));
        Assert.assertFalse(has(diagnostics, INCOMPLETE_SETUP_KEY));
    }

    /**
     * Returns true if {@code path}'s first four bytes are {@link BinaryStubBundle#MAGIC}, i.e. it
     * looks like a bundle rather than an ordinary per-file binary stub (whose raw bytes are instead
     * a GZIP header). Duplicates {@code AnnotationFileElementTypes#looksLikeBundle}, which is
     * private to that class; used here only to assert the test fixture itself is set up as
     * intended.
     *
     * @param path the file to check
     * @return true if {@code path} looks like a binary stub bundle
     * @throws IOException if {@code path} cannot be read
     */
    private static boolean looksLikeBundleMagic(Path path) throws IOException {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            return in.readInt() == BinaryStubBundle.MAGIC;
        }
    }

    /**
     * A fresh per-file binary also passes the framework's own differential check ({@code
     * -AbinaryStubDiffCheck}, from {@code BinaryStubDiffChecker} in {@code framework-test}), which
     * independently text-parses the same {@code .astub} file and compares every annotation it
     * applies against what the binary form applied. This is a second, more exhaustive layer of
     * verification than this test's own {@link #has} assertions: it would have caught the earlier
     * (rejected) bundle design that merged multiple files' compilation units into one write call,
     * which silently corrupted annotation resolution.
     */
    @Test
    public void freshPerFileBinaryPassesDiffCheck() throws IOException {
        Path stubsDir = tempDir.resolve("stubs");
        write(stubsDir.resolve("Lib.astub"), LIB_STUB_ENCRYPTED);
        BinaryStubFileGenerator.main(new String[] {stubsDir.toString(), stubsDir.toString()});

        List<Diagnostic<? extends JavaFileObject>> diagnostics =
                compile("-Astubs=" + stubsDir, "-AbinaryStubDiffCheck");
        Assert.assertFalse(
                "the binary stub's @Encrypted must have been applied",
                has(diagnostics, MISMATCH_KEY));
        assertNoDiffCheckMismatch(diagnostics);
    }

    /**
     * A fresh bundle also passes the differential check; see {@link
     * #freshPerFileBinaryPassesDiffCheck}.
     */
    @Test
    public void freshBundlePassesDiffCheck() throws IOException {
        Path stubsDir = tempDir.resolve("stubs");
        write(stubsDir.resolve("Lib.astub"), LIB_STUB_ENCRYPTED);
        BinaryStubFileGenerator.main(new String[] {"--bundle", stubsDir.toString()});

        List<Diagnostic<? extends JavaFileObject>> diagnostics =
                compile("-Astubs=" + stubsDir, "-AbinaryStubDiffCheck");
        Assert.assertFalse(
                "the bundle's @Encrypted must have been applied", has(diagnostics, MISMATCH_KEY));
        assertNoDiffCheckMismatch(diagnostics);
    }

    /**
     * Asserts that none of {@code diagnostics} is a {@code BinaryStubDiffChecker} mismatch report.
     * That checker reports via {@code SourceChecker.message(Kind, String)} with a literal {@code
     * "binary stub diff: "} prefix, not a message key, so the message text carries the prefix
     * regardless of {@code -Anomsgtext} (unlike {@link #has}'s keyed messages).
     *
     * @param diagnostics the diagnostics to search
     */
    private static void assertNoDiffCheckMismatch(
            List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
            String message = d.getMessage(null);
            Assert.assertFalse(
                    "the differential check (-AbinaryStubDiffCheck) found a mismatch between the"
                            + " binary and text forms of the stub: "
                            + message,
                    message != null && message.contains("binary stub diff"));
        }
    }

    /**
     * A fresh binary sibling entry inside a {@code .jar} passed to {@code -Astubs} is read and
     * applied, the JAR-entry analogue of {@link #freshPerFileBinaryIsApplied}.
     */
    @Test
    public void freshJarBinaryIsApplied() throws IOException {
        Path jarPath = tempDir.resolve("stubs.jar");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("Lib.astub", LIB_STUB_ENCRYPTED.getBytes(StandardCharsets.UTF_8));
        writeJar(jarPath, entries);
        BinaryStubFileGenerator.main(
                new String[] {tempDir.resolve("unused-output").toString(), jarPath.toString()});

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + jarPath);
        Assert.assertFalse(
                "the jar entry's @Encrypted must have been applied",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertFalse(
                "a fresh, complete binary setup must not warn", has(diagnostics, STALE_KEY));
        Assert.assertFalse(
                "a fresh, complete binary setup must not warn",
                has(diagnostics, INCOMPLETE_SETUP_KEY));
    }

    /**
     * A fresh jar entry binary also passes the differential check, exercising {@code jarEntryToURL}
     * (the {@code jar:} URL construction {@code -AbinaryStubDiffCheck} needs to re-read the entry's
     * text form); see {@link #freshPerFileBinaryPassesDiffCheck}.
     */
    @Test
    public void freshJarBinaryPassesDiffCheck() throws IOException {
        Path jarPath = tempDir.resolve("stubs.jar");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("Lib.astub", LIB_STUB_ENCRYPTED.getBytes(StandardCharsets.UTF_8));
        writeJar(jarPath, entries);
        BinaryStubFileGenerator.main(
                new String[] {tempDir.resolve("unused-output").toString(), jarPath.toString()});

        List<Diagnostic<? extends JavaFileObject>> diagnostics =
                compile("-Astubs=" + jarPath, "-AbinaryStubDiffCheck");
        Assert.assertFalse(
                "the jar entry's @Encrypted must have been applied",
                has(diagnostics, MISMATCH_KEY));
        assertNoDiffCheckMismatch(diagnostics);
    }

    /**
     * A binary sibling entry inside a {@code .jar} that no longer matches its {@code .astub}
     * entry's content (the jar was rebuilt with an edited entry, keeping the old sibling from
     * before) is detected as stale, falls back to text-parsing the edited content, and warns -- the
     * JAR-entry analogue of {@link #staleBinaryFallsBackToEditedText}.
     */
    @Test
    public void staleJarBinaryFallsBackToEditedText() throws IOException {
        Path jarPath = tempDir.resolve("stubs.jar");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("Lib.astub", LIB_STUB_ENCRYPTED.getBytes(StandardCharsets.UTF_8));
        writeJar(jarPath, entries);
        BinaryStubFileGenerator.main(
                new String[] {tempDir.resolve("unused-output").toString(), jarPath.toString()});
        byte[] staleBinEntry = readJarEntry(jarPath, "Lib.astub" + BinaryStubData.BIN_SUFFIX);

        // Rebuild the jar (JarOutputStream cannot append to an existing one) with an edited
        // .astub entry, keeping the old, now-stale binary sibling untouched.
        Map<String, byte[]> editedEntries = new LinkedHashMap<>();
        editedEntries.put("Lib.astub", LIB_STUB_UNANNOTATED.getBytes(StandardCharsets.UTF_8));
        editedEntries.put("Lib.astub" + BinaryStubData.BIN_SUFFIX, staleBinEntry);
        writeJar(jarPath, editedEntries);

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + jarPath);
        Assert.assertTrue(
                "a stale jar binary must fall back to the edited (unannotated) text, not the"
                        + " stale @Encrypted binary",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertTrue("a stale jar binary must be warned about", has(diagnostics, STALE_KEY));
    }

    /**
     * A {@code .jar} with a binary sibling entry for only some of its {@code .astub} entries still
     * applies the covered entry's binary form, text-parses the uncovered one, and warns about the
     * incomplete setup -- the JAR-entry analogue of {@link #incompleteBundleSetupWarns}. Also
     * confirms that a {@link JarEntryAnnotationFileResource} participates in this accounting now
     * that jar entries can have a binary form (earlier, only a {@link FileAnnotationFileResource}
     * did).
     */
    @Test
    public void incompleteJarSetupWarns() throws IOException {
        Path jarPath = tempDir.resolve("stubs.jar");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("Lib.astub", LIB_STUB_ENCRYPTED.getBytes(StandardCharsets.UTF_8));
        writeJar(jarPath, entries);
        BinaryStubFileGenerator.main(
                new String[] {tempDir.resolve("unused-output").toString(), jarPath.toString()});
        byte[] libBinEntry = readJarEntry(jarPath, "Lib.astub" + BinaryStubData.BIN_SUFFIX);

        // Rebuild the jar with Lib's entry and its binary sibling intact, plus a second,
        // unrelated .astub entry that was never covered by a generator run.
        Map<String, byte[]> allEntries = new LinkedHashMap<>();
        allEntries.put("Lib.astub", LIB_STUB_ENCRYPTED.getBytes(StandardCharsets.UTF_8));
        allEntries.put("Lib.astub" + BinaryStubData.BIN_SUFFIX, libBinEntry);
        allEntries.put("Other.astub", OTHER_STUB.getBytes(StandardCharsets.UTF_8));
        writeJar(jarPath, allEntries);

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + jarPath);
        Assert.assertFalse(
                "Lib's own binary entry must still have been applied",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertTrue(
                "a jar with only some entries covered by a binary form must warn",
                has(diagnostics, INCOMPLETE_SETUP_KEY));
    }

    /**
     * A {@code .jar} entry whose only "sibling" at the expected binary name is a directory entry
     * (e.g. {@code Lib.astub.bin.gz/}, not a real file) is treated as having no binary form at all,
     * not misread: {@link JarFile#getJarEntry} silently falls back to matching {@code name + "/"}
     * when the exact name is absent, so {@code parseCommandLineStubJarResource} must reject a
     * directory match explicitly. Also confirms this falls back to text parsing rather than
     * throwing, since a real archive with a stray directory entry at that path (e.g. from a
     * mis-packaged build) must still compile successfully.
     */
    @Test
    public void jarBinarySiblingDirectoryEntryIsIgnored() throws IOException {
        Path jarPath = tempDir.resolve("stubs.jar");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("Lib.astub", LIB_STUB_ENCRYPTED.getBytes(StandardCharsets.UTF_8));
        // A directory entry at exactly the path a real binary sibling would occupy, but not one.
        entries.put("Lib.astub" + BinaryStubData.BIN_SUFFIX + "/", new byte[0]);
        writeJar(jarPath, entries);

        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("-Astubs=" + jarPath);
        Assert.assertFalse(
                "text-parsing the .astub entry must still apply @Encrypted",
                has(diagnostics, MISMATCH_KEY));
        Assert.assertFalse(
                "a directory entry masquerading as a binary sibling must not be reported as"
                        + " stale -- it must be treated as simply absent",
                has(diagnostics, STALE_KEY));
    }

    /**
     * Writes a JAR file at {@code jarPath} (overwriting one if present) with the given entries.
     *
     * @param jarPath the path to write the JAR to
     * @param entries map from entry name to entry content
     * @throws IOException if the JAR cannot be written
     */
    private static void writeJar(Path jarPath, Map<String, byte[]> entries) throws IOException {
        try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(jarPath))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                jarOut.putNextEntry(new JarEntry(entry.getKey()));
                jarOut.write(entry.getValue());
                jarOut.closeEntry();
            }
        }
    }

    /**
     * Reads one entry's content from a JAR file.
     *
     * @param jarPath the JAR file to read from
     * @param entryName the entry to read
     * @return the entry's content
     * @throws IOException if the JAR or the entry cannot be read
     */
    private static byte[] readJarEntry(Path jarPath, String entryName) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(entryName);
            Assert.assertNotNull("no such entry: " + entryName, entry);
            try (InputStream in = jarFile.getInputStream(entry)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
                return out.toByteArray();
            }
        }
    }

    /**
     * Recursively deletes {@code file} (which may be a plain file or a directory).
     *
     * @param file the file or directory to delete
     */
    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
