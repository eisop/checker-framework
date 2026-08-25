package org.checkerframework.framework.stub;

// WARNING: framework.jar must work standalone, but stubifier classes are not bundled into it
// (they ship only inside checker.jar's minimized shadow jar; see framework/build.gradle's
// `implementation sourceSets.stubifier.output` dependency). Only reference compile-time
// constants of BinaryStubWriter from here (static final primitive/String fields with a constant
// initializer) -- javac inlines those into this class's own bytecode, so no runtime dependency
// on the stubifier is created. Never call a BinaryStubWriter method or read a non-constant field
// from this class; doing so would break framework.jar used on its own.
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
 * <p>A bundle is a container, not a merged {@link BinaryStubData}: each entry is the exact same
 * bytes a per-file {@code .astub.bin.gz} would contain for that source file, generated
 * independently (so each retains its own file's import resolution and class records; see {@link
 * BinaryStubData}). This constructor reads every entry's raw bytes into memory eagerly (a bundle
 * covers one {@code -Astubs} directory, which a command-line stub invocation processes in full
 * anyway); {@link #get} defers the more expensive step, parsing an individual entry into a {@link
 * BinaryStubData}, until requested.
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

    /**
     * Magic number identifying a binary stub bundle. The value is defined once in {@link
     * BinaryStubWriter#BUNDLE_MAGIC} and referenced here (the constant is inlined at compile time,
     * so there is no runtime dependency on the stubifier).
     */
    public static final int MAGIC = BinaryStubWriter.BUNDLE_MAGIC;

    /**
     * Format version of the bundle container. Defined once in {@link
     * BinaryStubWriter#BUNDLE_VERSION}.
     */
    public static final short VERSION = BinaryStubWriter.BUNDLE_VERSION;

    /**
     * File-name suffix appended to a source stub directory's name to name the bundle covering it
     * (e.g. a {@code -Astubs} directory named {@code mystubs} → sibling file {@code
     * mystubs.astub.bin.gz}). Defined once in {@link BinaryStubWriter#BUNDLE_SUFFIX}.
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
            entries = new HashMap<>((int) (count / 0.75f) + 1);
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
     * Returns the binary stub data for the entry at {@code relativePath}, parsing it on this call
     * (not cached: each entry is normally requested at most once per compilation, while a directory
     * passed to {@code -Astubs} is walked).
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
     * Returns true if the bundle has an entry for {@code relativePath}, without parsing it. Lets a
     * caller decide whether to do more expensive work (such as reading and hashing the
     * corresponding source file) before calling {@link #get}, which does parse.
     *
     * @param relativePath the entry's path, relative to the bundled directory, with {@code '/'} as
     *     separator
     * @return true if the bundle has an entry for {@code relativePath}
     */
    public boolean contains(String relativePath) {
        return entries.containsKey(relativePath);
    }
}
