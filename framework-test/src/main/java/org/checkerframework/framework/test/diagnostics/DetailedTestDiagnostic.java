package org.checkerframework.framework.test.diagnostics;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a detailed error/warning message reported by the Javac compiler. when the
 * -Adetailedmsgtext flag is given. By contrast, {@link TestDiagnostic} represents a simple expected
 * error/warning message in a Java test file or an error/warning reported by the Javac compiler
 * without the -Adetailedmsgtext flag.
 */
public class DetailedTestDiagnostic extends TestDiagnostic {

    /** Additional tokens that are part of the error message. */
    private final List<String> additionalTokens;

    /** The start position of the error in the source file. */
    private final Integer startPosition;

    /** The end position of the error in the source file. */
    private final Integer endPosition;

    /** An error key that usually appears between parentheses in diagnostic messages. */
    private final String errorKey;

    /** Constructor that sets the immutable fields of this diagnostic. */
    public DetailedTestDiagnostic(
            Path file,
            long lineNo,
            DiagnosticKind kind,
            String errorKey,
            List<String> additionalTokens,
            int startPosition,
            int endPosition,
            String readableMessage,
            boolean isFixable) {
        super(file, lineNo, kind, readableMessage, isFixable);

        this.additionalTokens = additionalTokens;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.errorKey = errorKey;
    }

    public List<String> getAdditionalTokens() {
        return additionalTokens;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public String getErrorKey() {
        return errorKey;
    }

    /**
     * Equality is compared without isFixable/omitParentheses.
     *
     * @return true if this and otherObj are equal according to additionalTokens, startPosition,
     *     endPosition, errorKey, and the fields of the superclass.
     */
    @Override
    public boolean equals(@Nullable Object otherObj) {
        if (!(otherObj instanceof DetailedTestDiagnostic)) {
            return false;
        }
        DetailedTestDiagnostic other = (DetailedTestDiagnostic) otherObj;
        return super.equals(other)
                && additionalTokens.equals(other.additionalTokens)
                && startPosition.equals(other.startPosition)
                && endPosition.equals(other.endPosition)
                && errorKey.equals(other.errorKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getFilename(),
                getLineNumber(),
                getKind(),
                getMessage(),
                additionalTokens,
                startPosition,
                endPosition,
                errorKey);
    }

    /**
     * Returns a representation of this diagnostic as if it appeared in a diagnostics file.
     *
     * @return a representation of this diagnostic as if it appeared in a diagnostics file
     */
    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner("$$");

        sj.add(errorKey);
        if (additionalTokens != null) {
            sj.add(Integer.toString(additionalTokens.size()));
            for (String token : additionalTokens) {
                sj.add(token);
            }
        } else {
            sj.add("0");
        }

        sj.add(String.format("(%d, %d)", startPosition, endPosition));
        sj.add(getMessage());
        return sj.toString();
    }
}
