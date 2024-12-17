package org.checkerframework.framework.test.diagnostics;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a detailed error/warning message reported by the Checker Framework when the {@code
 * -Adetailedmsgtext} flag is used. By contrast, {@link TestDiagnostic} represents a simple expected
 * error/warning message in a Java test file or an error/warning reported by the Java compiler
 * without the {@code -Adetailedmsgtext} flag.
 */
public class DetailedTestDiagnostic extends TestDiagnostic {

    /** Additional tokens that are part of the diagnostic message. */
    protected final List<String> additionalTokens;

    /** The start position of the diagnostic in the source file. */
    protected final long startPosition;

    /** The end position of the diagnostic in the source file. */
    protected final long endPosition;

    /**
     * Create a new instance.
     *
     * @param file the file in which the diagnostic occurred
     * @param lineNo the line number in the file at which the diagnostic occurred
     * @param kind the kind of diagnostic (error or warning)
     * @param messageKey a message key that usually appears between parentheses in diagnostic
     *     messages
     * @param additionalTokens additional tokens that are part of the diagnostic message
     * @param startPosition the start position of the diagnostic in the source file
     * @param endPosition the end position of the diagnostic in the source file
     * @param readableMessage a human-readable message describing the diagnostic
     * @param isFixable whether the diagnostic is fixable
     */
    public DetailedTestDiagnostic(
            Path file,
            long lineNo,
            DiagnosticKind kind,
            String messageKey,
            List<String> additionalTokens,
            long startPosition,
            long endPosition,
            String readableMessage,
            boolean isFixable) {
        super(file, lineNo, kind, messageKey, readableMessage, isFixable);

        this.additionalTokens = additionalTokens;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    /**
     * The additional tokens that are part of the diagnostic message.
     *
     * @return the additional tokens
     */
    public List<String> getAdditionalTokens() {
        return additionalTokens;
    }

    /**
     * The start position of the diagnostic in the source file.
     *
     * @return the start position
     */
    public long getStartPosition() {
        return startPosition;
    }

    /**
     * The end position of the diagnostic in the source file.
     *
     * @return the end position
     */
    public long getEndPosition() {
        return endPosition;
    }

    /**
     * Equality is compared without isFixable and messageKeyParens.
     *
     * @return true if this and otherObj are equal according to additionalTokens, startPosition,
     *     endPosition, and equality of the superclass.
     */
    @Override
    public boolean equals(@Nullable Object otherObj) {
        if (!(otherObj instanceof DetailedTestDiagnostic)) {
            return false;
        }
        DetailedTestDiagnostic other = (DetailedTestDiagnostic) otherObj;
        return super.equals(other)
                && additionalTokens.equals(other.additionalTokens)
                && startPosition == other.startPosition
                && endPosition == other.endPosition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), additionalTokens, startPosition, endPosition);
    }

    /**
     * Returns a representation of this diagnostic as if it appeared as a detailed message.
     *
     * @return a representation of this diagnostic as if it appeared as a detailed message
     * @see
     *     org.checkerframework.framework.source.SourceChecker#detailedMsgTextPrefix(Object,String,Object[])
     */
    @Override
    public String toString() {
        // Keep in sync with SourceChecker.DETAILS_SEPARATOR.
        StringJoiner sj = new StringJoiner(" $$ ");

        sj.add("(" + messageKey + ")");
        if (additionalTokens != null) {
            sj.add(Integer.toString(additionalTokens.size()));
            for (String token : additionalTokens) {
                sj.add(token);
            }
        } else {
            sj.add("0");
        }

        sj.add(String.format("( %d, %d )", startPosition, endPosition));
        sj.add(getMessage());
        return sj.toString();
    }
}
