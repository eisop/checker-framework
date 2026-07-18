package org.checkerframework.framework.stubifier;

import org.checkerframework.framework.stub.BinaryStubData;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Regression test for a bug where {@code JavaStubifier}'s {@code BinaryStubWriter} was held in a
 * {@code private static final} field: since {@code main} can process several directories in one
 * invocation, and a {@code BinaryStubWriter} accumulates every class passed to {@code process} in
 * instance fields with no reset between calls, reusing one writer across directories made each
 * directory's own {@code annotated-jdk.bin.gz} also contain every earlier directory's classes.
 */
public class JavaStubifierTest {

    /**
     * Writes a one-line public class declaration to a new file in {@code dir}.
     *
     * @param dir the directory to write into
     * @param className the name of the class, also used as the file name
     * @throws IOException if the file cannot be written
     */
    private static void writeClass(Path dir, String className) throws IOException {
        Files.write(
                dir.resolve(className + ".java"),
                ("public class " + className + " {}").getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Loads the binary stub data written to {@code annotated-jdk.bin.gz} in {@code dir}.
     *
     * @param dir the directory containing the binary stub file
     * @return the loaded binary stub data
     * @throws IOException if the file cannot be read
     */
    private static BinaryStubData load(Path dir) throws IOException {
        File file = new File(dir.toFile(), BinaryStubWriter.OUTPUT_FILENAME);
        // BinaryStubData's constructor already GZIP-decompresses internally, so pass it the raw
        // file stream (not pre-wrapped in a GZIPInputStream, which would double-decompress).
        try (InputStream in = new FileInputStream(file)) {
            return new BinaryStubData(in);
        }
    }

    /**
     * Recursively deletes a directory tree.
     *
     * @param dir the directory to delete
     * @throws IOException if deletion fails
     */
    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    /**
     * A binary stub that cannot be written must abort the run, not leave a stale or truncated file
     * behind.
     *
     * <p>{@code process} used to print the {@code IOException} to stderr and return. Whatever
     * {@code annotated-jdk.bin.gz} an earlier run had left in the directory then shipped, and the
     * checker prefers that file over the text stubs whenever it exists, so its (wrong) contents
     * would be applied in place of the JDK's real annotations, silently. {@code
     * BinaryStubFileGenerator} already treats the same file-level failure as fatal.
     *
     * <p>The write is made to fail by putting a directory where the output file belongs.
     */
    @Test
    public void unwritableBinaryStubAbortsRatherThanLeavingAStaleFile() throws IOException {
        Path dir = Files.createTempDirectory("javastubifiertest");
        try {
            writeClass(dir, "Kept");
            // A FileOutputStream cannot open a directory for writing.
            Files.createDirectory(dir.resolve(BinaryStubWriter.OUTPUT_FILENAME));

            RuntimeException e =
                    Assert.assertThrows(
                            RuntimeException.class,
                            () -> JavaStubifier.main(new String[] {dir.toString()}));
            Assert.assertTrue(
                    "the failure must name the binary stub it could not write, but was: "
                            + e.getMessage(),
                    e.getMessage().contains(BinaryStubWriter.OUTPUT_FILENAME));
        } finally {
            deleteRecursively(dir);
        }
    }

    /**
     * Verifies that processing two directories in one {@code main()} invocation does not leak
     * classes from the first directory into the second one's binary stub output; see the class
     * documentation for the underlying bug.
     *
     * @throws IOException if a temporary file or directory cannot be created, written, or read
     */
    @Test
    public void eachDirectoryGetsOnlyItsOwnClasses() throws IOException {
        Path dir1 = Files.createTempDirectory("stubifier-test-1");
        Path dir2 = Files.createTempDirectory("stubifier-test-2");
        try {
            writeClass(dir1, "JavaStubifierTestFooA");
            writeClass(dir2, "JavaStubifierTestFooB");

            JavaStubifier.main(new String[] {dir1.toString(), dir2.toString()});

            BinaryStubData data1 = load(dir1);
            BinaryStubData data2 = load(dir2);

            Assert.assertTrue(
                    "dir1's output must contain its own class",
                    data1.classes.containsKey("JavaStubifierTestFooA"));
            Assert.assertFalse(
                    "dir1's output must not contain dir2's class",
                    data1.classes.containsKey("JavaStubifierTestFooB"));
            Assert.assertTrue(
                    "dir2's output must contain its own class",
                    data2.classes.containsKey("JavaStubifierTestFooB"));
            Assert.assertFalse(
                    "dir2's output must not accumulate dir1's class from the earlier directory"
                            + " in the same main() invocation",
                    data2.classes.containsKey("JavaStubifierTestFooA"));
        } finally {
            deleteRecursively(dir1);
            deleteRecursively(dir2);
        }
    }

    /**
     * Regression test for the "does this file have any interesting declaration" check. {@code
     * RecordDeclaration} extends {@code TypeDeclaration}, so the simplified {@code
     * cu.findAll(TypeDeclaration.class).isEmpty()} check correctly treats a record-only file as
     * non-empty and keeps it. This test verifies that a file containing only a record declaration
     * is not deleted (previously it was deleted because BinaryStubWriter did not support records,
     * but now it does).
     */
    @Test
    public void recordOnlyFileIsKept() throws IOException {
        Path dir = Files.createTempDirectory("stubifier-test-record");
        try {
            Path recordFile = dir.resolve("JavaStubifierTestRecord.java");
            Files.write(
                    recordFile,
                    "public record JavaStubifierTestRecord(int x) {}"
                            .getBytes(StandardCharsets.UTF_8));

            JavaStubifier.main(new String[] {dir.toString()});

            Assert.assertTrue(
                    "a record-only file must be kept, since BinaryStubWriter now supports records",
                    Files.exists(recordFile));
        } finally {
            deleteRecursively(dir);
        }
    }

    /** Source text of a class whose field is annotated with an annotation not on the classpath. */
    private static final String UNLOADABLE_ANNOTATION_SOURCE =
            "import com.example.NotOnClasspath;\n"
                    + "public class NeedsAnnotation {\n"
                    + "  public @NotOnClasspath Object f;\n"
                    + "}\n";

    /**
     * By default, an unloadable annotation aborts the whole run, and the failure names both the
     * annotation and the source file that was being processed -- the file path used to be missing,
     * which made finding the offending file among thousands require grepping the whole tree.
     */
    @Test
    public void unloadableAnnotationFailsWithFilePathInMessage() throws IOException {
        Path dir = Files.createTempDirectory("stubifier-test-unloadable");
        try {
            Path file = dir.resolve("NeedsAnnotation.java");
            Files.write(file, UNLOADABLE_ANNOTATION_SOURCE.getBytes(StandardCharsets.UTF_8));

            RuntimeException e =
                    Assert.assertThrows(
                            RuntimeException.class,
                            () -> JavaStubifier.main(new String[] {dir.toString()}));
            Assert.assertTrue(
                    "the failure must name the unloadable annotation, but was: " + e.getMessage(),
                    e.getMessage().contains("com.example.NotOnClasspath"));
            Assert.assertTrue(
                    "the failure must name the file being processed, but was: " + e.getMessage(),
                    e.getMessage().contains(file.toString()));
        } finally {
            deleteRecursively(dir);
        }
    }

    /**
     * With {@link JavaStubifier#SKIP_UNLOADABLE_ANNOTATIONS_FLAG}, the same input succeeds instead:
     * the unloadable annotation is dropped from the binary stub output (so the field ends up with
     * no annotations, and the annotated-JDK writer then omits its record entirely, per {@link
     * BinaryStubWriterTest#jdkWriterDemotesUnannotatedMethodRecords}'s analogous case for methods),
     * and a warning naming the annotation and the file is printed to stderr.
     */
    @Test
    public void skipUnloadableAnnotationsFlagOmitsAnnotationAndSucceeds() throws IOException {
        Path dir = Files.createTempDirectory("stubifier-test-skip");
        try {
            Path file = dir.resolve("NeedsAnnotation.java");
            Files.write(file, UNLOADABLE_ANNOTATION_SOURCE.getBytes(StandardCharsets.UTF_8));

            PrintStream originalErr = System.err;
            ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
            // The three-argument PrintStream(OutputStream, boolean, Charset) constructor is not
            // available on Java 8, which this project's compileTestJava must still target; the
            // String-charset-name overload is the Java-8-compatible equivalent, and is explicit
            // about the charset (unlike the default-charset constructor), so no behavior is lost.
            @SuppressWarnings("JdkObsolete")
            PrintStream captureStream =
                    new PrintStream(capturedErr, true, StandardCharsets.UTF_8.name());
            System.setErr(captureStream);
            try {
                JavaStubifier.main(
                        new String[] {
                            JavaStubifier.SKIP_UNLOADABLE_ANNOTATIONS_FLAG, dir.toString()
                        });
            } finally {
                System.setErr(originalErr);
            }
            // Same Java-8-compatibility reasoning as above: ByteArrayOutputStream.toString(Charset)
            // is not available on Java 8.
            @SuppressWarnings("JdkObsolete")
            String warnings = capturedErr.toString(StandardCharsets.UTF_8.name());
            Assert.assertTrue(
                    "the warning must name the dropped annotation, but was: " + warnings,
                    warnings.contains("NotOnClasspath"));
            Assert.assertTrue(
                    "the warning must name the file it was dropped from, but was: " + warnings,
                    warnings.contains(file.toString()));

            BinaryStubData data = load(dir);
            BinaryStubData.ClassRecord cr = data.classes.get("NeedsAnnotation");
            Assert.assertNotNull("the class record must still be written", cr);
            Assert.assertEquals(
                    "the field must be omitted: its only annotation was dropped, leaving it"
                            + " unannotated, and the annotated-JDK writer drops unannotated field"
                            + " records",
                    0,
                    cr.fields.length);
        } finally {
            deleteRecursively(dir);
        }
    }
}
