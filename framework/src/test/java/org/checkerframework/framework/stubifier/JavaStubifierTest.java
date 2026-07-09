package org.checkerframework.framework.stubifier;

import org.checkerframework.framework.stub.BinaryStubData;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
     * Regression test for a single-pass rewrite of the "does this file have any interesting
     * declaration" check ({@code cu.findAll(ClassOrInterfaceDeclaration.class).isEmpty() &&
     * cu.findAll(AnnotationDeclaration.class).isEmpty() && cu.findAll(EnumDeclaration.class)
     * .isEmpty()}, replaced by a single {@code findAll(TypeDeclaration.class, predicate)}): {@code
     * RecordDeclaration} also extends {@code TypeDeclaration}, so a naive replacement that dropped
     * the predicate would treat a record-only file as non-empty and keep it, whereas
     * BinaryStubWriter does not support records and the file should still be deleted as empty.
     */
    @Test
    public void recordOnlyFileIsStillTreatedAsEmpty() throws IOException {
        Path dir = Files.createTempDirectory("stubifier-test-record");
        try {
            Path recordFile = dir.resolve("JavaStubifierTestRecord.java");
            Files.write(
                    recordFile,
                    "public record JavaStubifierTestRecord(int x) {}"
                            .getBytes(StandardCharsets.UTF_8));

            JavaStubifier.main(new String[] {dir.toString()});

            Assert.assertFalse(
                    "a record-only file must still be deleted as empty, since"
                            + " BinaryStubWriter does not support records",
                    Files.exists(recordFile));
        } finally {
            deleteRecursively(dir);
        }
    }
}
