package org.checkerframework.framework.stub;

// WARNING: only reference compile-time constants of BinaryStubWriter from here; never call a
// method or read a non-constant field. See the warning at the top of BinaryStubData.
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.stubifier.BinaryStubWriter;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory index of a binary stub <em>bundle</em>: a single file combining the binary form of
 * every {@code .astub} file under one {@code -Astubs} directory, as written by {@code
 * BinaryStubFileGenerator}'s {@code --bundle} mode. Conventionally named with the {@link #SUFFIX}
 * suffix, as a sibling of the directory it covers.
 *
 * <p>A bundle is a container, not a merged {@link BinaryStubData}: each entry holds the exact bytes
 * a per-file {@code .astub.bin.gz} would, generated independently, so each entry has its own
 * constant and annotation pools whose indices are meaningful only within that entry. Entry bytes
 * are read eagerly; {@link #get} parses an entry into a {@link BinaryStubData} only when requested.
 *
 * <p>The binary format consists of:
 *
 * <ol>
 *   <li>A 4-byte magic number ({@link #MAGIC}).
 *   <li>A 2-byte version number.
 *   <li>An entry count.
 *   <li>For each entry: its slash-separated path (relative to the bundled directory), a 4-byte byte
 *       length, and that many raw bytes -- the complete, independently gzip-compressed contents a
 *       per-file {@code .astub.bin.gz} would hold for that source file.
 * </ol>
 *
 * @see BinaryStubData
 * @see BinaryStubReader
 */
public class BinaryStubBundle {

    /** Magic number identifying a binary stub bundle. */
    public static final int MAGIC = BinaryStubWriter.BUNDLE_MAGIC;

    /** Format version of the bundle container. */
    public static final short VERSION = BinaryStubWriter.BUNDLE_VERSION;

    /**
     * File-name suffix appended to a source stub directory's name to name the bundle covering it
     * (e.g. a {@code -Astubs} directory named {@code mystubs} → sibling file {@code
     * mystubs.astub.bin.gz}).
     */
    public static final String SUFFIX = BinaryStubWriter.BUNDLE_SUFFIX;

    /**
     * Map from an entry's slash-separated relative path to its raw (still gzip-compressed) bytes.
     */
    private final Map<String, byte[]> entries;

    /**
     * Reads a bundle's directory of entries from the given stream.
     *
     * @param in the input stream to read from; the stream is closed when this constructor returns
     * @throws IOException if the stream cannot be read or contains an invalid/unsupported format
     */
    public BinaryStubBundle(InputStream in) throws IOException {
        try (DataInputStream dataIn = new DataInputStream(new BufferedInputStream(in))) {
            if (dataIn.readInt() != MAGIC) {
                throw new IOException("Invalid binary stub bundle magic number");
            }
            short version = dataIn.readShort();
            if (version != VERSION) {
                throw new IOException("Unsupported binary stub bundle version: " + version);
            }
            int count = BinaryStubData.readCount(dataIn, "binary stub bundle entry count");
            // Not pre-sized from `count`: it is file-supplied, so malformed input could otherwise
            // request an arbitrarily large initial table.
            entries = new HashMap<>();
            for (int i = 0; i < count; i++) {
                String path = dataIn.readUTF();
                int length = BinaryStubData.readCount(dataIn, "binary stub bundle entry length");
                byte[] bytes = new byte[length];
                dataIn.readFully(bytes);
                entries.put(path, bytes);
            }
        }
    }

    /**
     * Returns the binary stub data for the entry at {@code relativePath}, parsing it on this call.
     * The result is not cached.
     *
     * @param relativePath the entry's path, relative to the bundled directory, with {@code '/'} as
     *     separator
     * @return the entry's binary stub data, or null if the bundle has no entry for that path
     * @throws IOException if the entry's bytes cannot be parsed as binary stub data
     */
    public @Nullable BinaryStubData get(String relativePath) throws IOException {
        byte[] bytes = entries.get(relativePath);
        if (bytes == null) {
            return null;
        }
        try {
            return BinaryStubData.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new IOException(
                    "Malformed binary stub bundle entry " + relativePath + ": " + e, e);
        }
    }

    /**
     * Returns true if the bundle has an entry for {@code relativePath}, without parsing it, unlike
     * {@link #get}.
     *
     * @param relativePath the entry's path, relative to the bundled directory, with {@code '/'} as
     *     separator
     * @return true if the bundle has an entry for {@code relativePath}
     */
    public boolean contains(String relativePath) {
        return entries.containsKey(relativePath);
    }
}
