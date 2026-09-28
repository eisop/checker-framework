package org.checkerframework.framework.stub;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tests that a damaged binary stub bundle is reported as a malformed file, not a crash. Mirrors
 * {@link BinaryStubDataTest}, which does the same for the per-file format that a bundle's entries
 * are made of; see that class's documentation for why the files here are hand-built rather than
 * produced by {@code BinaryStubFileGenerator} and then damaged.
 *
 * <p>Unlike a per-file binary stub, a bundle's own container is not GZIP-compressed (only each
 * entry's bytes are; see {@link BinaryStubBundle}'s class documentation), so these bytes are built
 * directly, uncompressed.
 */
public class BinaryStubBundleTest {

    /**
     * Returns a stream of bytes whose header (magic number and version) is valid.
     *
     * @param body writes the bytes that follow the magic number and version
     * @return the bytes
     * @throws IOException if the bytes cannot be written
     */
    private static InputStream bundle(BodyWriter body) throws IOException {
        return raw(
                out -> {
                    out.writeInt(BinaryStubBundle.MAGIC);
                    out.writeShort(BinaryStubBundle.VERSION);
                    body.write(out);
                });
    }

    /**
     * Returns a stream of {@code body}'s bytes, with no header of its own: for a file whose magic
     * number or version is itself what is wrong.
     *
     * @param body writes the file's bytes
     * @return the bytes
     * @throws IOException if the bytes cannot be written
     */
    private static InputStream raw(BodyWriter body) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            body.write(out);
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    /** Writes the part of a binary stub bundle that follows the magic number and version. */
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
     * Asserts that reading {@code in} as a binary stub bundle fails with an {@code IOException}
     * whose message contains {@code expectedMessage}.
     *
     * @param in the bundle bytes to read
     * @param expectedMessage a substring of the expected exception message, or null to accept any
     *     (a truncated file fails with an EOFException, whose message is null)
     */
    private static void assertMalformed(InputStream in, @Nullable String expectedMessage) {
        try {
            new BinaryStubBundle(in);
            Assert.fail("expected an IOException, but the bundle was read successfully");
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
            // instead of falling back to per-file siblings or text parsing.
            Assert.fail("expected an IOException, but got " + e);
        }
    }

    /** A negative entry count must be rejected, not passed to a negative-size allocation. */
    @Test
    public void negativeEntryCountIsMalformed() throws IOException {
        assertMalformed(
                bundle(out -> out.writeInt(-1)), "implausible binary stub bundle entry count");
    }

    /** An absurdly large entry count must be rejected, not allocated. */
    @Test
    public void hugeEntryCountIsMalformed() throws IOException {
        assertMalformed(
                bundle(out -> out.writeInt(Integer.MAX_VALUE)),
                "implausible binary stub bundle entry count");
    }

    /** A negative entry length must be rejected, not passed to {@code new byte[-1]}. */
    @Test
    public void negativeEntryLengthIsMalformed() throws IOException {
        assertMalformed(
                bundle(
                        out -> {
                            out.writeInt(1); // one entry
                            out.writeUTF("Foo.astub");
                            out.writeInt(-1); // implausible length
                        }),
                "implausible binary stub bundle entry length");
    }

    /** An absurdly large entry length must be rejected, not allocated. */
    @Test
    public void hugeEntryLengthIsMalformed() throws IOException {
        assertMalformed(
                bundle(
                        out -> {
                            out.writeInt(1); // one entry
                            out.writeUTF("Foo.astub");
                            out.writeInt(Integer.MAX_VALUE); // implausible length
                        }),
                "implausible binary stub bundle entry length");
    }

    /** A file that stops in the middle of an entry must be rejected. */
    @Test
    public void truncatedFileIsMalformed() throws IOException {
        assertMalformed(
                bundle(
                        out -> {
                            out.writeInt(1); // one entry
                            out.writeUTF("Foo.astub");
                            out.writeInt(10); // claims ten bytes, but supplies none
                        }),
                null);
    }

    /** A file that is not a binary stub bundle at all must be rejected. */
    @Test
    public void wrongMagicIsMalformed() throws IOException {
        assertMalformed(
                raw(
                        out -> {
                            out.writeInt(0xDEADBEEF);
                            out.writeShort(BinaryStubBundle.VERSION);
                        }),
                "Invalid binary stub bundle magic number");
    }

    /** A bundle written by a different version of the format must be rejected. */
    @Test
    public void wrongVersionIsMalformed() throws IOException {
        assertMalformed(
                raw(
                        out -> {
                            out.writeInt(BinaryStubBundle.MAGIC);
                            out.writeShort(BinaryStubBundle.VERSION + 1);
                        }),
                "Unsupported binary stub bundle version");
    }
}
