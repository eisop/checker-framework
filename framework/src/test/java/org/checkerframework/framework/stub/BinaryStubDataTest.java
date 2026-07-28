package org.checkerframework.framework.stub;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tests that a damaged binary stub file is reported as a malformed file, not as a crash.
 *
 * <p>{@code AnnotationFileElementTypes.loadBinaryStubData} turns an {@code IOException} into a
 * fallback to text parsing, and anything else into "please report a Checker Framework bug". A
 * corrupt file -- a truncated download, a bad transfer, a bit flip -- must therefore surface as an
 * {@code IOException}. That is the whole reason {@code BinaryStubData.readCount} and {@code
 * readStringIndex} exist, so each is exercised here with the input it is meant to reject: a count
 * that cannot be a real count, and an index that points outside the string pool.
 *
 * <p>The files are hand-built rather than produced by the writer and then damaged: the point is to
 * reject bytes the writer would never emit, and building them directly says exactly which byte is
 * wrong.
 */
public class BinaryStubDataTest {

    /**
     * Returns a GZIP-compressed stream of a binary stub file whose header is valid.
     *
     * @param body writes the bytes that follow the magic number and version
     * @return the compressed bytes
     * @throws IOException if the bytes cannot be written
     */
    private static InputStream binaryStub(BodyWriter body) throws IOException {
        return compressed(
                out -> {
                    out.writeInt(BinaryStubData.MAGIC);
                    out.writeShort(BinaryStubData.VERSION);
                    body.write(out);
                });
    }

    /**
     * Returns a GZIP-compressed stream of {@code body}'s bytes, with no header of its own: for a
     * file whose magic number or version is itself what is wrong.
     *
     * @param body writes the file's bytes
     * @return the compressed bytes
     * @throws IOException if the bytes cannot be written
     */
    private static InputStream compressed(BodyWriter body) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            body.write(out);
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    /** Writes the part of a binary stub file that follows the magic number and version. */
    private interface BodyWriter {
        /**
         * Writes the body.
         *
         * @param out the stream to write to
         * @throws IOException if the bytes cannot be written
         */
        void write(DataOutputStream out) throws IOException;
    }

    /**
     * Asserts that reading {@code in} as binary stub data fails with an {@code IOException} whose
     * message contains {@code expectedMessage}.
     *
     * @param in the binary stub data to read
     * @param expectedMessage a substring of the expected exception message, or null to accept any
     *     (a truncated file fails with an EOFException, whose message is null)
     */
    private static void assertMalformed(InputStream in, @Nullable String expectedMessage) {
        try {
            new BinaryStubData(in);
            Assert.fail("expected an IOException, but the file was read successfully");
        } catch (IOException e) {
            if (expectedMessage == null) {
                // A truncated file fails with an EOFException, whose message is null.
                return;
            }
            Assert.assertTrue(
                    "expected a message containing \""
                            + expectedMessage
                            + "\", but got: "
                            + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains(expectedMessage));
        } catch (RuntimeException e) {
            // The bug this whole test guards against: a corrupt file that crashes the compilation
            // (NegativeArraySizeException, OutOfMemoryError, ArrayIndexOutOfBoundsException, ...)
            // instead of falling back to text parsing.
            Assert.fail("expected an IOException, but got " + e);
        }
    }

    /** A negative count must be rejected, not passed to {@code new String[-1]}. */
    @Test
    public void negativeCountIsMalformed() throws IOException {
        assertMalformed(binaryStub(out -> out.writeInt(-1)), "implausible string pool size");
    }

    /** An absurdly large count must be rejected, not allocated. */
    @Test
    public void hugeCountIsMalformed() throws IOException {
        assertMalformed(
                binaryStub(out -> out.writeInt(Integer.MAX_VALUE)), "implausible string pool size");
    }

    /**
     * A string-pool index that points outside the pool must be rejected while the file is read.
     *
     * <p>The bad index here is a <em>field name</em>, and every other byte of the file is valid.
     * That is the case that matters: a field's name index is not dereferenced while reading (unlike
     * a class's, which indexes {@code classes} immediately), but only later, when the class's
     * records are applied -- outside any {@code IOException} handler. Left unvalidated, this file
     * parses successfully, is cached, and then throws {@code ArrayIndexOutOfBoundsException} the
     * first time the class is used, crashing the compilation.
     */
    @Test
    public void stringPoolIndexOutsideThePoolIsMalformed() throws IOException {
        assertMalformed(
                binaryStub(
                        out -> {
                            out.writeInt(1); // string pool: one entry, index 0
                            out.writeUTF("Foo");
                            out.writeInt(0); // annotation pool: empty
                            out.writeInt(1); // one class record
                            out.writeInt(0); // class name: "Foo"
                            out.writeInt(0); // no outer class
                            out.writeByte(BinaryStubData.ClassRecord.KIND_CLASS_OR_INTERFACE);
                            out.writeShort(0); // no declaration annotations
                            out.writeShort(1); // one field
                            out.writeInt(9999); //   its name: an index outside the pool
                            out.writeShort(0); //    no declaration annotations
                            out.writeShort(0); //    no type annotations
                            out.writeShort(0); // no methods
                            out.writeShort(0); // no presence-only method signatures
                            out.writeShort(0); // no type parameters
                            out.writeInt(0); // no annotated packages
                            out.writeInt(0); // no annotated modules
                        }),
                "outside the string pool");
    }

    /**
     * An annotation-pool index that points outside the pool must be rejected while the file is
     * read.
     *
     * <p>Similar to string pool indices, an annotation index is only dereferenced later during
     * application. Left unvalidated, an out-of-bounds index throws ArrayIndexOutOfBoundsException
     * later on, crashing the compiler.
     */
    @Test
    public void annotationPoolIndexOutsideThePoolIsMalformed() throws IOException {
        assertMalformed(
                binaryStub(
                        out -> {
                            out.writeInt(1); // string pool: one entry, index 0
                            out.writeUTF("Foo");
                            out.writeInt(0); // annotation pool: empty
                            out.writeInt(1); // one class record
                            out.writeInt(0); // class name: "Foo"
                            out.writeInt(0); // no outer class
                            out.writeByte(BinaryStubData.ClassRecord.KIND_CLASS_OR_INTERFACE);
                            out.writeShort(1); // one declaration annotation
                            out.writeInt(9999); //   its index: outside the annotation pool
                            out.writeShort(0); // no fields
                            out.writeShort(0); // no methods
                            out.writeShort(0); // no presence-only method signatures
                            out.writeShort(0); // no type parameters
                            out.writeInt(0); // no annotated packages
                            out.writeInt(0); // no annotated modules
                        }),
                "outside the annotation pool");
    }

    /** A file that stops in the middle of a record must be rejected. */
    @Test
    public void truncatedFileIsMalformed() throws IOException {
        assertMalformed(
                binaryStub(
                        out -> {
                            out.writeInt(2); // string pool: claims two entries
                            out.writeUTF("Foo"); // but supplies only one
                        }),
                null);
    }

    /** A file that is not a binary stub file at all must be rejected. */
    @Test
    public void wrongMagicIsMalformed() throws IOException {
        assertMalformed(
                compressed(
                        out -> {
                            out.writeInt(0xDEADBEEF);
                            out.writeShort(BinaryStubData.VERSION);
                        }),
                "Invalid magic number");
    }

    /** A file written by a different version of the format must be rejected. */
    @Test
    public void wrongVersionIsMalformed() throws IOException {
        assertMalformed(
                compressed(
                        out -> {
                            out.writeInt(BinaryStubData.MAGIC);
                            out.writeShort(BinaryStubData.VERSION + 1);
                        }),
                "Unsupported version");
    }
}
