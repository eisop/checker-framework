package org.checkerframework.framework.util.visualize;

/**
 * Stores an inclusive range [(startLine, startCol), (endLine, endCol)] in the source code to which
 * a piece of type information refers. All indices are 0-based since LSP uses 0-based positions.
 */
public class TypeOccurrenceRange {
    /** 0-based line number of the start position. */
    private final long startLine;

    /** 0-based column number of the start position. */
    private final long startCol;

    /** 0-based line number of the end position. */
    private final long endLine;

    /** 0-based column number of the end position. */
    private final long endCol;

    /**
     * Constructs a new {@link TypeOccurrenceRange} with the given position information.
     *
     * @param startLine 0-based line number of the start position
     * @param startCol 0-based column number of the start position
     * @param endLine 0-based line number of the end position
     * @param endCol 0-based column number of the end position
     */
    private TypeOccurrenceRange(long startLine, long startCol, long endLine, long endCol) {
        this.startLine = startLine;
        this.startCol = startCol;
        this.endLine = endLine;
        this.endCol = endCol;
    }

    /**
     * Constructs a new {@link TypeOccurrenceRange} with the given position information.
     *
     * @param startLine 0-based line number of the start position
     * @param startCol 0-based column number of the start position
     * @param endLine 0-based line number of the end position
     * @param endCol 0-based column number of the end position
     * @return a new {@link TypeOccurrenceRange} with the given position information
     */
    public static TypeOccurrenceRange of(long startLine, long startCol, long endLine, long endCol) {
        return new TypeOccurrenceRange(startLine, startCol, endLine, endCol);
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d, %d)", startLine, startCol, endLine, endCol);
    }
}
