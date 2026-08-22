package org.checkerframework.framework.stubifier;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.StubUnit;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Build-time generator that pre-parses {@code .astub} files into the binary stub format read by
 * {@code org.checkerframework.framework.stub.BinaryStubReader}. Has two modes:
 *
 * <ul>
 *   <li>{@code BinaryStubFileGenerator <outputDir> <inputRoot>...}: each input root is a single
 *       {@code .astub} file, a directory searched recursively for {@code .astub} files, or a {@code
 *       .jar} file searched for {@code .astub} entries. For every one found, writes a sibling-named
 *       {@code .astub.bin.gz} file or entry: under the output root for a file or directory root
 *       (preserving the relative path for a directory, so the binary is packaged as a sibling
 *       resource of the text stub), or -- ignoring the output root -- as a new entry inside the JAR
 *       itself for a {@code .jar} root (see {@link #generateForJar}). One {@code .astub} file or
 *       entry always maps to one such binary. This is the mode the build uses to pre-parse a
 *       checker's built-in stub files.
 *   <li>{@code BinaryStubFileGenerator --bundle <inputRoot>...}: for each input root (a directory),
 *       combines the binary form of every {@code .astub} file under it into one <em>bundle</em>
 *       file, written beside the root itself (not under any output directory) as {@code
 *       org.checkerframework.framework.stub.BinaryStubBundle#SUFFIX} names it. This is the mode a
 *       user runs by hand to pre-parse a whole {@code -Astubs} directory into a single file; see
 *       {@code org.checkerframework.framework.stub.AnnotationFileElementTypes}, which looks for
 *       this bundle beside a directory passed to {@code -Astubs} before falling back to per-file
 *       siblings or text parsing.
 * </ul>
 *
 * At checker startup, {@code AnnotationFileElementTypes} loads the binary form of a stub file if
 * present and falls back to text parsing otherwise.
 *
 * <p>A file is skipped -- no binary is emitted for it, so the checker text-parses it -- if it
 * cannot be parsed, or fails to serialize. Skipping is always safe; it only forgoes the speedup for
 * that file.
 */
public class BinaryStubFileGenerator {

    /** Do not instantiate; this is a main class. */
    private BinaryStubFileGenerator() {}

    /**
     * The main entry point; see the class documentation for the two modes.
     *
     * @param args {@code --bundle} followed by one or more input root directories, or an output
     *     directory followed by one or more input roots (each a directory, a single {@code .astub}
     *     file, or a {@code .jar} file)
     * @throws IOException if walking an input root, or reading or writing its output, fails
     */
    public static void main(String[] args) throws IOException {
        if (args.length >= 1 && args[0].equals("--bundle")) {
            if (args.length < 2) {
                System.err.println("Usage: BinaryStubFileGenerator --bundle <inputRoot>...");
                System.exit(2);
                return;
            }
            boolean anyCollision = false;
            for (int i = 1; i < args.length; i++) {
                if (!generateBundle(Paths.get(args[i]))) {
                    anyCollision = true;
                }
            }
            printValidationNote();
            if (anyCollision) {
                System.exit(1);
            }
            return;
        }

        if (args.length < 2) {
            System.err.println("Usage: BinaryStubFileGenerator <outputDir> <inputRoot>...");
            System.exit(2);
            return;
        }
        Path outRoot = Paths.get(args[0]);
        int written = 0;
        int skipped = 0;
        for (int i = 1; i < args.length; i++) {
            Path inRoot = Paths.get(args[i]);
            if (Files.isDirectory(inRoot)) {
                for (Path astub : findAstubs(inRoot)) {
                    if (generateOne(astub, inRoot.relativize(astub), outRoot)) {
                        written++;
                    } else {
                        skipped++;
                    }
                }
                // A -Astubs directory is searched recursively for .astub entries inside a
                // nested .jar too (AnnotationFileUtil#addAnnotationFilesToList), so this must
                // reach those the same way, or a nested jar's entries could never get a binary
                // form through this directory root and would permanently trigger the
                // "incomplete binary stub setup" warning with no way to clear it.
                for (Path nestedJar : findJars(inRoot)) {
                    int[] counts = generateForJar(nestedJar);
                    written += counts[0];
                    skipped += counts[1];
                }
            } else if (isAstubFile(inRoot)) {
                Path fileName = inRoot.getFileName();
                if (generateOne(inRoot, fileName == null ? inRoot : fileName, outRoot)) {
                    written++;
                } else {
                    skipped++;
                }
            } else if (isJarFile(inRoot)) {
                // outRoot is not used here: unlike a directory or a loose file, a JAR's binaries
                // are written into the JAR itself, in place -- see generateForJar.
                int[] counts = generateForJar(inRoot);
                written += counts[0];
                skipped += counts[1];
            } else if (Files.exists(inRoot)) {
                // Only for a root that exists but is not one of the recognized kinds (e.g. a
                // typo'd path that happens to hit an unrelated file): a root that does not exist
                // at all is not worth a message, since this tool's callers (including the build's
                // own generateBinaryStubFiles task) routinely pass an optional root, such as a
                // resources directory a given subproject may not have, that is fine to skip
                // silently.
                System.err.println(
                        "BinaryStubFileGenerator: not a directory, .astub file, or .jar file,"
                                + " skipping: "
                                + inRoot);
            }
        }
        System.out.printf(
                "BinaryStubFileGenerator: wrote %d binary stub files, skipped %d.%n",
                written, skipped);
        printValidationNote();
    }

    /**
     * Prints a reminder that this tool does not validate a stub file's content -- it only checks
     * that it parses -- so a binary form silently carries forward any "declared method does not
     * exist" or similar problem that a real checker run would otherwise report.
     */
    private static void printValidationNote() {
        System.out.println(
                "Note: this tool only checks that a stub file parses; it does not check that its"
                        + " declarations resolve against your library's real classes. Run your"
                        + " checker once with -Astubs=<path> -AstubWarnIfNotFound (text-parsed,"
                        + " since that option disables the binary path) to catch a mismatch before"
                        + " relying on the binary form day to day.");
    }

    /**
     * Returns true if {@code path} is a plain file whose name ends in {@code .astub}.
     *
     * @param path the path to check
     * @return true if {@code path} is a {@code .astub} file
     */
    private static boolean isAstubFile(Path path) {
        return path.toString().endsWith(".astub") && Files.isRegularFile(path);
    }

    /**
     * Returns true if {@code path} is a plain file whose name ends in {@code .jar}.
     *
     * @param path the path to check
     * @return true if {@code path} is a {@code .jar} file
     */
    private static boolean isJarFile(Path path) {
        return path.toString().endsWith(".jar") && Files.isRegularFile(path);
    }

    /**
     * Generates the binary form of every {@code .astub} entry in {@code jarPath}, and writes each
     * one into {@code jarPath} itself as a new sibling entry (e.g. entry {@code path/Lib.astub}
     * gets a new {@code path/Lib.astub.bin.gz} entry beside it) -- modifying the JAR file in place,
     * the same "write new siblings, touch nothing else" operation the directory case performs on a
     * filesystem tree. An existing sibling entry from a previous run is replaced, not duplicated.
     *
     * <p>There is no bundle mode for a JAR (see the class documentation's {@code --bundle}
     * paragraph): a JAR is already one file regardless of how many internal entries it has, so the
     * filesystem-clutter problem a bundle solves for a directory of many loose files does not
     * apply.
     *
     * @param jarPath the JAR file to process
     * @return the number of binaries written and the number of files skipped, as {@code {written,
     *     skipped}}
     * @throws IOException if the JAR file cannot be read or written
     */
    private static int[] generateForJar(Path jarPath) throws IOException {
        // Absolute, so writeEntriesIntoJar's use of jarPath.getParent() is never null: a bare
        // relative name with no directory component (e.g. "project-stubs.jar") has a null
        // getParent() otherwise, which Files.createTempFile rejects with a
        // NullPointerException.
        jarPath = jarPath.toAbsolutePath();
        Map<String, byte[]> newEntries = new LinkedHashMap<>();
        // Sibling entries (from a previous run) to delete, for a file that no longer parses --
        // otherwise a fixed-then-broken-again .astub entry would keep its last-good binary
        // sibling forever, with no way to clear it (unlike the directory case's
        // deleteStaleOutput, or a bundle's regeneration, which both drop such an entry).
        List<String> staleEntries = new ArrayList<>();
        int skipped = 0;
        List<String> astubEntryNames = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".astub")) {
                    astubEntryNames.add(entry.getName());
                }
            }
            Collections.sort(astubEntryNames);
            for (String name : astubEntryNames) {
                byte[] sourceBytes;
                JarEntry astubEntry = jarFile.getJarEntry(name);
                try (InputStream in = jarFile.getInputStream(astubEntry)) {
                    sourceBytes = readAllBytes(in);
                }
                byte[] blob = generateBlob(jarPath + "!" + name, sourceBytes);
                if (blob == null) {
                    skipped++;
                    staleEntries.add(name + BinaryStubWriter.BIN_SUFFIX);
                    continue;
                }
                newEntries.put(name + BinaryStubWriter.BIN_SUFFIX, blob);
            }
        }
        if (!newEntries.isEmpty() || !staleEntries.isEmpty()) {
            writeEntriesIntoJar(jarPath, newEntries, staleEntries);
        }
        return new int[] {newEntries.size(), skipped};
    }

    /**
     * Writes {@code newEntries} into {@code jarPath} as new ZIP entries, replacing any existing
     * entry with the same name (e.g. a stale sibling from a previous run), and deletes {@code
     * staleEntries} (an entry that is no longer regenerable, e.g. because its source no longer
     * parses).
     *
     * @param jarPath the JAR file to modify
     * @param newEntries map from ZIP entry name to that entry's content
     * @param staleEntries names of entries to delete, if present
     * @throws IOException if the JAR file cannot be opened or written
     */
    private static void writeEntriesIntoJar(
            Path jarPath, Map<String, byte[]> newEntries, List<String> staleEntries)
            throws IOException {
        // Modify a temporary copy, then move it into place atomically, so a crash or a full disk
        // mid-write cannot leave jarPath as a corrupt ZIP -- the same rationale as
        // generateBundle's identical pattern. A copy is used, rather than modifying jarPath's own
        // ZIP structure directly, because the ZIP filesystem provider offers no way to stage its
        // changes to a separate file and commit them atomically at the end.
        Path tmp = Files.createTempFile(jarPath.getParent(), jarPath.getFileName() + ".", ".tmp");
        try {
            Files.copy(jarPath, tmp, StandardCopyOption.REPLACE_EXISTING);
            URI uri = URI.create("jar:" + tmp.toUri());
            try (FileSystem zipfs = FileSystems.newFileSystem(uri, new HashMap<String, Object>())) {
                for (String staleEntry : staleEntries) {
                    Files.deleteIfExists(zipfs.getPath(staleEntry));
                }
                for (Map.Entry<String, byte[]> entry : newEntries.entrySet()) {
                    Path entryPath = zipfs.getPath(entry.getKey());
                    Path parent = entryPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.write(
                            entryPath,
                            entry.getValue(),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
            Files.move(
                    tmp,
                    jarPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * Returns every {@code .astub} file under {@code inRoot}, in a deterministic order.
     *
     * @param inRoot the root to walk
     * @return the {@code .astub} files found under {@code inRoot}, sorted
     * @throws IOException if walking {@code inRoot} fails
     */
    private static List<Path> findAstubs(Path inRoot) throws IOException {
        try (Stream<Path> walk = Files.walk(inRoot)) {
            return walk.filter(BinaryStubFileGenerator::isAstubFile)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Returns every {@code .jar} file under {@code inRoot}, in a deterministic order.
     *
     * @param inRoot the root to walk
     * @return the {@code .jar} files found under {@code inRoot}, sorted
     * @throws IOException if walking {@code inRoot} fails
     */
    private static List<Path> findJars(Path inRoot) throws IOException {
        try (Stream<Path> walk = Files.walk(inRoot)) {
            return walk.filter(BinaryStubFileGenerator::isJarFile)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Generates the binary form of one {@code .astub} file and writes it to {@code outRoot}/{@code
     * relativeName}{@link BinaryStubWriter#BIN_SUFFIX}.
     *
     * @param astub the stub file to process
     * @param relativeName {@code astub}'s path relative to its input root (or just its file name,
     *     if the input root is {@code astub} itself)
     * @param outRoot the output root the binary is written under
     * @return true if a binary was written, false if the file was skipped
     */
    private static boolean generateOne(Path astub, Path relativeName, Path outRoot) {
        Path out = outRoot.resolve(relativeName + BinaryStubWriter.BIN_SUFFIX);
        byte[] sourceBytes;
        try {
            sourceBytes = Files.readAllBytes(astub);
        } catch (IOException e) {
            System.err.println(
                    "BinaryStubFileGenerator: skipping "
                            + astub
                            + " (falls back to text parsing): "
                            + e);
            deleteStaleOutput(out);
            return false;
        }
        byte[] blob = generateBlob(astub, sourceBytes);
        if (blob == null) {
            deleteStaleOutput(out);
            return false;
        }
        try {
            Files.createDirectories(out.getParent());
            Files.write(out, blob);
            return true;
        } catch (IOException e) {
            System.err.println(
                    "BinaryStubFileGenerator: skipping "
                            + astub
                            + " (falls back to text parsing): "
                            + e);
            deleteStaleOutput(out);
            return false;
        }
    }

    /**
     * Combines the binary form of every {@code .astub} file under {@code inRoot} into one bundle
     * file, written beside {@code inRoot} itself.
     *
     * @param inRoot the input root to bundle
     * @return true if the bundle was written (even if it bundles zero files, e.g. an empty
     *     directory); false if a file already exists where the bundle would be written and it is
     *     not itself a bundle -- refusing to overwrite it, so the caller reports failure
     * @throws IOException if walking {@code inRoot}, or writing the bundle, fails
     */
    private static boolean generateBundle(Path inRoot) throws IOException {
        if (!Files.isDirectory(inRoot)) {
            System.err.println("BinaryStubFileGenerator: not a directory, skipping: " + inRoot);
            return true;
        }
        // Absolute, so bundlePath.getParent() below is never null: a bare relative name with no
        // directory component (e.g. "project-stubs", exactly as the manual's own example
        // invokes this) has a null getParent() otherwise, which Files.createTempFile rejects
        // with a NullPointerException.
        inRoot = inRoot.toAbsolutePath();
        Path fileName = inRoot.getFileName();
        Path bundlePath =
                inRoot.resolveSibling(
                        (fileName == null ? inRoot.toString() : fileName.toString())
                                + BinaryStubWriter.BUNDLE_SUFFIX);
        if (Files.exists(bundlePath) && !looksLikeBundle(bundlePath)) {
            System.err.println(
                    "BinaryStubFileGenerator: refusing to overwrite "
                            + bundlePath
                            + ": it already exists and is not a binary stub bundle");
            return false;
        }

        Map<String, byte[]> entries = new LinkedHashMap<>();
        int skipped = 0;
        for (Path astub : findAstubs(inRoot)) {
            byte[] sourceBytes;
            try {
                sourceBytes = Files.readAllBytes(astub);
            } catch (IOException e) {
                System.err.println(
                        "BinaryStubFileGenerator: skipping "
                                + astub
                                + " (falls back to text parsing): "
                                + e);
                skipped++;
                continue;
            }
            byte[] blob = generateBlob(astub, sourceBytes);
            if (blob == null) {
                skipped++;
                continue;
            }
            String relativePath = inRoot.relativize(astub).toString().replace('\\', '/');
            entries.put(relativePath, blob);
        }

        // Write to a temporary file in the same directory, then move it into place atomically, so
        // a crash or a full disk mid-write cannot leave a truncated file at bundlePath that still
        // starts with BUNDLE_MAGIC (which looksLikeBundle, and the reader, would otherwise accept
        // as a real, if damaged, bundle).
        Path tmp =
                Files.createTempFile(
                        bundlePath.getParent(), bundlePath.getFileName() + ".", ".tmp");
        try {
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(tmp))) {
                out.writeInt(BinaryStubWriter.BUNDLE_MAGIC);
                out.writeShort(BinaryStubWriter.BUNDLE_VERSION);
                out.writeInt(entries.size());
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeInt(entry.getValue().length);
                    out.write(entry.getValue());
                }
            }
            Files.move(
                    tmp,
                    bundlePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
        System.out.printf(
                "BinaryStubFileGenerator: wrote bundle %s with %d entries, skipped %d.%n",
                bundlePath, entries.size(), skipped);
        return true;
    }

    /**
     * Returns true if {@code path} starts with {@link BinaryStubWriter#BUNDLE_MAGIC}, i.e. it looks
     * like a binary stub bundle rather than some unrelated file (including a per-file binary stub,
     * which can coincidentally share a bundle's target file name; see {@link
     * BinaryStubWriter#BUNDLE_SUFFIX}).
     *
     * @param path the file to check
     * @return true if {@code path}'s first four bytes are {@link BinaryStubWriter#BUNDLE_MAGIC};
     *     false if they are not, or if {@code path} cannot be read at all (e.g. too short)
     */
    private static boolean looksLikeBundle(Path path) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            return in.readInt() == BinaryStubWriter.BUNDLE_MAGIC;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Parses {@code astub}'s bytes and serializes them into the binary stub format, including a
     * fingerprint of {@code sourceBytes} (see {@code
     * org.checkerframework.framework.stub.BinaryStubData#sourceLength}). Shared by every source of
     * {@code .astub} bytes this class reads (a filesystem file, or a JAR entry).
     *
     * @param description the stub's location, for diagnostics only
     * @param sourceBytes the stub file's raw content
     * @return the binary form of {@code sourceBytes}, or null if it cannot be parsed or serialized
     */
    private static byte @Nullable [] generateBlob(Object description, byte[] sourceBytes) {
        try {
            // Mirror JavaParserUtil.parseStubUnit: a stub file may contain several `package`
            // sections, which the stub parser represents as several compilation units.
            ParserConfiguration configuration = new ParserConfiguration();
            // Same language level as JavaStubifier.DEFAULT_LANGUAGE_LEVEL, which is in this same
            // source set. That constant in turn matches JavaParserUtil.DEFAULT_LANGUAGE_LEVEL,
            // which the text parser uses; that duplication can't be unified further here because
            // JavaParserUtil is not on the stubifier classpath (framework main depends on the
            // stubifier source set's output, not the other way around, and framework.jar ships no
            // stubifier classes), so enum constants can't compile-time-fold across that boundary.
            configuration.setLanguageLevel(JavaStubifier.DEFAULT_LANGUAGE_LEVEL);
            configuration.setStoreTokens(false);
            configuration.setLexicalPreservationEnabled(false);
            configuration.setAttributeComments(false);
            configuration.setDetectOriginalLineSeparator(false);
            configuration.setPreprocessUnicodeEscapes(true);
            ParseResult<StubUnit> parseResult =
                    new JavaParser(configuration)
                            .parseStubUnit(new ByteArrayInputStream(sourceBytes));
            if (!parseResult.isSuccessful() || !parseResult.getResult().isPresent()) {
                System.err.println(
                        "BinaryStubFileGenerator: cannot parse "
                                + description
                                + " (falls back to text parsing): "
                                + parseResult.getProblems());
                return null;
            }
            List<CompilationUnit> cus = parseResult.getResult().get().getCompilationUnits();
            BinaryStubWriter writer = new BinaryStubWriter();
            writer.processStubUnit(cus);
            writer.setSourceFingerprint(sourceBytes);
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            writer.writeTo(bytesOut);
            return bytesOut.toByteArray();
        } catch (Exception e) {
            System.err.println(
                    "BinaryStubFileGenerator: skipping "
                            + description
                            + " (falls back to text parsing): "
                            + e);
            return null;
        }
    }

    /**
     * Reads all of {@code in}'s remaining bytes.
     *
     * <p>Equivalent to {@code InputStream.readAllBytes()} (Java 9+), avoided because this project
     * is meant to run under a Java 8 runtime. Duplicated in {@code
     * org.checkerframework.framework.stub.AnnotationFileElementTypes}, which cannot call this
     * class's methods (see the warning at the top of {@code
     * org.checkerframework.framework.stub.BinaryStubData}: {@code framework} main code must not
     * create a runtime dependency on the {@code stubifier} source set).
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

    /**
     * Deletes a binary stub output file left over from a previous run, now that its source can no
     * longer be represented in binary form. A stale binary must not ship, so a failure to delete it
     * makes the build fail loudly rather than silently packaging outdated content.
     *
     * @param out the output file to delete if present
     */
    private static void deleteStaleOutput(Path out) {
        try {
            Files.deleteIfExists(out);
        } catch (IOException cleanupFailure) {
            throw new RuntimeException("Could not delete stale binary stub " + out, cleanupFailure);
        }
    }
}
