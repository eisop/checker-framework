package org.checkerframework.framework.test.diagnostics;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents an expected error/warning message in a Java test file or an error/warning reported by
 * the Javac compiler. By contrast, {@link TestDiagnosticLine} represents a set of TestDiagnostics,
 * all of which were read from the same line of a file.
 *
 * @see JavaDiagnosticReader
 * @see TestDiagnosticLine
 */
public class TestDiagnostic {

    /** The path to the test file. */
    protected final Path file;

    /** The base file name of the test file. */
    protected final String filename;

    /** The line number of the diagnostic output. */
    protected final long lineNumber;

    /** The diagnostic kind of the output. */
    protected final DiagnosticKind kind;

    /** The full error message. */
    protected final String message;

    /**
     * The error key that usually appears between parentheses in diagnostic messages. Parentheses
     * are removed and field errorkeyparens indicates whether they were present.
     */
    protected final String errorkey;

    /** Whether the error key had parentheses around it. */
    protected final boolean errorkeyparens;

    /** Whether this diagnostic should no longer be reported after whole program inference. */
    protected final boolean isFixable;

    /**
     * Basic constructor that sets the immutable fields of this diagnostic.
     *
     * @param file the path to the test file
     * @param lineNumber the line number of the diagnostic output
     * @param kind the diagnostic kind of the output
     * @param message the full error message
     * @param isFixable whether WPI can fix the test
     */
    public TestDiagnostic(
            Path file, long lineNumber, DiagnosticKind kind, String message, boolean isFixable) {
        this.file = file;
        this.filename =
                file.getFileName() != null ? file.getFileName().toString() : file.toString();
        this.lineNumber = lineNumber;
        this.kind = kind;
        this.message = message;
        this.isFixable = isFixable;

        if (keepFullMessage(message)) {
            this.errorkey = message;
            this.errorkeyparens = false;
        } else {
            String[] msgSplit = this.message.split(System.lineSeparator(), 2);
            String firstline = msgSplit[0];
            int open = firstline.indexOf("(");
            int close = firstline.indexOf(")");
            if (open == 0 && close > open) {
                this.errorkey = firstline.substring(open + 1, close).trim();
                this.errorkeyparens = true;
            } else {
                this.errorkey = firstline;
                this.errorkeyparens = false;
            }
        }
    }

    /**
     * Determine whether the full error message should be used as error key. This is useful to
     * ensure e.g. stack traces are fully shown.
     *
     * @param message the full message
     * @return whether the full error message should be used
     */
    public static boolean keepFullMessage(String message) {
        return message.contains("unexpected Throwable")
                || message.contains("Compilation unit")
                || message.contains("OutOfMemoryError");
    }

    /**
     * The path to the test file.
     *
     * @return the path to the test file
     */
    public Path getFile() {
        return file;
    }

    /**
     * The base file name of the test file.
     *
     * @return the base file name of the test file
     */
    public String getFilename() {
        return filename;
    }

    /**
     * The line number of the diagnostic output.
     *
     * @return the line number of the diagnostic output
     */
    public long getLineNumber() {
        return lineNumber;
    }

    /**
     * The diagnostic kind of the output.
     *
     * @return the diagnostic kind of the output
     */
    public DiagnosticKind getKind() {
        return kind;
    }

    /**
     * The full error message.
     *
     * @return the full error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Whether WPI can fix the test.
     *
     * @return whether WPI can fix the test
     */
    public boolean isFixable() {
        return isFixable;
    }

    /**
     * Equality is compared based the file name, not the full path, on the errorkey, not the full
     * error message, and without considering isFixable and errorkeyparens.
     *
     * @return true if this and otherObj are equal according to file, lineNumber, kind, and errorkey
     */
    @Override
    public boolean equals(@Nullable Object otherObj) {
        if (otherObj == null || otherObj.getClass() != TestDiagnostic.class) {
            return false;
        }

        TestDiagnostic other = (TestDiagnostic) otherObj;
        return other.filename.equals(this.filename)
                && other.lineNumber == lineNumber
                && other.kind == this.kind
                && other.errorkey.equals(this.errorkey);
    }

    @Override
    public int hashCode() {
        // Only filename, not file, and only errorkey, not message, not isFixable, not
        // errorkeyparens.
        return Objects.hash(filename, lineNumber, kind, errorkey);
    }

    /**
     * Returns a representation of this diagnostic as if it appeared in a diagnostics file. This
     * uses only the base file name, not the full path, and only the error key, not the full
     * message. Field {@link errorkeyparens} influences whether the error key is output in
     * parentheses.
     *
     * @return a representation of this diagnostic as if it appeared in a diagnostics file
     */
    @Override
    public String toString() {
        if (errorkeyparens) {
            return filename + ":" + lineNumber + ": " + kind.parseString + ": (" + errorkey + ")";
        } else {
            return filename + ":" + lineNumber + ": " + kind.parseString + ": " + errorkey;
        }
    }

    /**
     * Returns the internal representation of this, formatted.
     *
     * @return the internal representation of this, formatted
     */
    public String repr() {
        return String.format(
                "[TestDiagnostic: file=%s, lineNumber=%d, kind=%s, message=%s]",
                file, lineNumber, kind, message);
    }
}
