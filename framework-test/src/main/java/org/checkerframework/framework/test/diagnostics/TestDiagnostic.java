package org.checkerframework.framework.test.diagnostics;

import java.nio.file.Path;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an expected error/warning message in a Java test file or an error/warning reported by
 * the Java compiler. By contrast, {@link TestDiagnosticLine} represents a set of TestDiagnostics,
 * all of which were read from the same line of a file. Subclass {@link DetailedTestDiagnostic} is
 * used when the Checker Framework is invoked with the {@code -Adetailedmsgtext} flag.
 *
 * @see JavaDiagnosticReader
 * @see TestDiagnosticLine
 * @see DetailedTestDiagnostic
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

  /** The full diagnostic message. */
  protected final String message;

  /**
   * The message key that usually appears between parentheses or square brackets in diagnostic
   * messages. The delimiters are removed and field messageKeyDelimiters records which were present.
   */
  protected final String messageKey;

  /**
   * The delimiters that were around the message key: {@code "()"}, {@code "[]"}, or the empty
   * string if there were none. Both forms are accepted, in expected diagnostics and in the compiler
   * output, and they are equal.
   */
  protected final String messageKeyDelimiters;

  /** Whether this diagnostic should no longer be reported after whole program inference. */
  protected final boolean isFixable;

  /**
   * Basic constructor that sets the immutable fields of this diagnostic.
   *
   * @param file the path to the test file
   * @param lineNumber the line number of the diagnostic output
   * @param kind the diagnostic kind of the output
   * @param messageKey the message key
   * @param message the full diagnostic message
   * @param isFixable whether WPI can fix the test
   */
  public TestDiagnostic(
      Path file,
      long lineNumber,
      DiagnosticKind kind,
      String messageKey,
      String message,
      boolean isFixable) {
    this.file = file;
    this.filename = file.getFileName() != null ? file.getFileName().toString() : file.toString();
    this.lineNumber = lineNumber;
    this.kind = kind;
    this.message = message;
    this.isFixable = isFixable;

    String[] unwrapped = unwrapKey(messageKey);
    this.messageKey = unwrapped[0];
    this.messageKeyDelimiters = unwrapped[1];
  }

  /**
   * Basic constructor that sets the immutable fields of this diagnostic.
   *
   * @param file the path to the test file
   * @param lineNumber the line number of the diagnostic output
   * @param kind the diagnostic kind of the output
   * @param message the full diagnostic message
   * @param isFixable whether WPI can fix the test
   */
  public TestDiagnostic(
      Path file, long lineNumber, DiagnosticKind kind, String message, boolean isFixable) {
    this.file = file;
    this.filename = file.getFileName() != null ? file.getFileName().toString() : file.toString();
    this.lineNumber = lineNumber;
    this.kind = kind;
    this.message = message;
    this.isFixable = isFixable;

    if (keepFullMessage(message)) {
      this.messageKey = message;
      this.messageKeyDelimiters = "";
    } else {
      String firstline;
      // There might be a mismatch between the System.lineSeparator() and the diagnostic
      // message, so manually check both options.
      int lineSepPos = this.message.indexOf("\r\n");
      if (lineSepPos == -1) {
        lineSepPos = this.message.indexOf("\n");
      }
      if (lineSepPos != -1) {
        firstline = this.message.substring(0, lineSepPos).trim();
      } else {
        firstline = this.message;
      }

      String[] unwrapped = unwrapKey(firstline);
      this.messageKey = unwrapped[0];
      this.messageKeyDelimiters = unwrapped[1];
    }
  }

  /**
   * Removes the delimiters around a message key. A key in parentheses, as in {@code (key)}, and a
   * key in square brackets that is the whole text, as in {@code [key]}, are recognized; the square
   * brackets of other text such as {@code [unchecked] unchecked cast} are not.
   *
   * @param text a message key, possibly in parentheses or in square brackets, or the first line of
   *     a diagnostic
   * @return a 2-element array: the key without delimiters, and the delimiters that were removed
   *     ({@code "()"}, {@code "[]"}, or {@code ""})
   */
  private static String[] unwrapKey(String text) {
    // Keep the parentheses rule as it was: the text starts with "(" and it contains ")".
    int open = text.indexOf("(");
    int close = text.indexOf(")");
    if (open == 0 && close > open) {
      return new String[] {text.substring(open + 1, close).trim(), "()"};
    }
    String trimmed = text.trim();
    if (trimmed.length() > 2
        && trimmed.charAt(0) == '['
        && trimmed.indexOf(']') == trimmed.length() - 1) {
      return new String[] {trimmed.substring(1, trimmed.length() - 1).trim(), "[]"};
    }
    return new String[] {text, ""};
  }

  /**
   * Determine whether the full diagnostic message should be used as message key. This is useful to
   * ensure e.g. stack traces are fully shown.
   *
   * @param message the full message
   * @return whether the full diagnostic message should be used
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
   * The message key, without surrounding parentheses or square brackets.
   *
   * @return the message key
   */
  public String getMessageKey() {
    return messageKey;
  }

  /**
   * The full diagnostic message.
   *
   * @return the full diagnostic message
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
   * Equality is compared based on the file name, not the full path, on the messageKey, not the full
   * message, and without considering isFixable and messageKeyDelimiters. The runtime class of the
   * argument must match {@code this.getClass()} exactly.
   *
   * @return true if this and otherObj are equal according to file, lineNumber, kind, and messageKey
   */
  @Override
  public boolean equals(@Nullable Object otherObj) {
    if (otherObj == null || otherObj.getClass() != this.getClass()) {
      return false;
    }

    TestDiagnostic other = (TestDiagnostic) otherObj;
    return other.filename.equals(this.filename)
        && other.lineNumber == lineNumber
        && other.kind == this.kind
        && other.messageKey.equals(this.messageKey);
  }

  @Override
  public int hashCode() {
    // Only filename, not file, and only messageKey, not message, not isFixable, not
    // messageKeyDelimiters.
    return Objects.hash(filename, lineNumber, kind, messageKey);
  }

  /**
   * Returns a representation of this diagnostic as if it appeared in a diagnostics file. This uses
   * only the base file name, not the full path, and only the message key, not the full message.
   * Field {@link #messageKeyDelimiters} determines whether the message key is output in
   * parentheses, in square brackets, or without delimiters.
   *
   * @return a representation of this diagnostic as if it appeared in a diagnostics file
   */
  @Override
  public String toString() {
    String open = messageKeyDelimiters.isEmpty() ? "" : messageKeyDelimiters.substring(0, 1);
    String close = messageKeyDelimiters.isEmpty() ? "" : messageKeyDelimiters.substring(1);
    return filename + ":" + lineNumber + ": " + kind.parseString + ": " + open + messageKey + close;
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
