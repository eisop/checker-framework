package org.checkerframework.framework.test.diagnostics;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.IPair;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/** A set of utilities and factory methods useful for working with TestDiagnostics. */
public class TestDiagnosticUtils {

    /** How the diagnostics appear in Java source files. */
    public static final String DIAGNOSTIC_IN_JAVA_REGEX =
            "\\s*(?<kind>error|fixable-error|warning|fixable-warning|Note|other):\\s*(?<message>[\\s\\S]*)";

    /** Pattern compiled from {@link #DIAGNOSTIC_IN_JAVA_REGEX}. */
    public static final Pattern DIAGNOSTIC_IN_JAVA_PATTERN =
            Pattern.compile(DIAGNOSTIC_IN_JAVA_REGEX);

    /** How the diagnostic warnings appear in Java source files. */
    public static final String DIAGNOSTIC_WARNING_IN_JAVA_REGEX =
            "\\s*warning:\\s*(?<message>[\\s\\S]*)";

    /** Pattern compiled from {@link #DIAGNOSTIC_WARNING_IN_JAVA_REGEX}. */
    public static final Pattern DIAGNOSTIC_WARNING_IN_JAVA_PATTERN =
            Pattern.compile(DIAGNOSTIC_WARNING_IN_JAVA_REGEX);

    /** How the diagnostics appear in javax tools diagnostics from the compiler. */
    public static final String DIAGNOSTIC_REGEX =
            "(?<linenogroup>:(?<lineno>\\d+):)?" + DIAGNOSTIC_IN_JAVA_REGEX;

    /** Pattern compiled from {@link #DIAGNOSTIC_REGEX}. */
    public static final Pattern DIAGNOSTIC_PATTERN = Pattern.compile(DIAGNOSTIC_REGEX);

    /** How the diagnostic warnings appear in javax tools diagnostics from the compiler. */
    public static final String DIAGNOSTIC_WARNING_REGEX =
            "(?<linenogroup>:(?<lineno>\\d+):)?" + DIAGNOSTIC_WARNING_IN_JAVA_REGEX;

    /** Pattern compiled from {@link #DIAGNOSTIC_WARNING_REGEX}. */
    public static final Pattern DIAGNOSTIC_WARNING_PATTERN =
            Pattern.compile(DIAGNOSTIC_WARNING_REGEX);

    /** How the diagnostics appear in diagnostic files (.out). */
    public static final String DIAGNOSTIC_FILE_REGEX = ".+\\.java" + DIAGNOSTIC_REGEX;

    /** Pattern compiled from {@link #DIAGNOSTIC_FILE_REGEX}. */
    public static final Pattern DIAGNOSTIC_FILE_PATTERN = Pattern.compile(DIAGNOSTIC_FILE_REGEX);

    /** How the diagnostic warnings appear in diagnostic files (.out). */
    public static final String DIAGNOSTIC_FILE_WARNING_REGEX =
            ".+\\.java" + DIAGNOSTIC_WARNING_REGEX;

    /** Pattern compiled from {@link #DIAGNOSTIC_FILE_WARNING_REGEX}. */
    public static final Pattern DIAGNOSTIC_FILE_WARNING_PATTERN =
            Pattern.compile(DIAGNOSTIC_FILE_WARNING_REGEX);

    /**
     * Instantiate the diagnostic based on a string that would appear in diagnostic files (i.e.
     * files that only contain line after line of expected diagnostics).
     *
     * @param stringFromDiagnosticFile a single diagnostic string to parse
     * @return a new TestDiagnostic
     */
    public static TestDiagnostic fromDiagnosticFileString(String stringFromDiagnosticFile) {
        return fromPatternMatching(
                DIAGNOSTIC_FILE_PATTERN,
                DIAGNOSTIC_WARNING_IN_JAVA_PATTERN,
                // Important to use "" to make input of expected warnings easy.
                Paths.get(""),
                null,
                stringFromDiagnosticFile);
    }

    /**
     * Instantiate the diagnostic from a string that would appear in a Java file, e.g.: "error:
     * (message)"
     *
     * @param filename the file containing the diagnostic (and the error)
     * @param lineNumber the line number of the line immediately below the diagnostic comment in the
     *     Java file
     * @param stringFromJavaFile the string containing the diagnostic
     * @return a new TestDiagnostic
     */
    public static TestDiagnostic fromJavaFileComment(
            String filename, long lineNumber, String stringFromJavaFile) {
        return fromPatternMatching(
                DIAGNOSTIC_IN_JAVA_PATTERN,
                DIAGNOSTIC_WARNING_IN_JAVA_PATTERN,
                Paths.get(filename),
                lineNumber,
                stringFromJavaFile);
    }

    /**
     * Instantiate a diagnostic from output produced by the Java compiler. The resulting diagnostic
     * is never fixable and always has parentheses.
     *
     * @param diagnosticString the compiler diagnostics string
     * @return the corresponding test diagnostic
     */
    public static TestDiagnostic fromJavaxToolsDiagnostic(String diagnosticString) {
        // It would be nice not to parse this from the diagnostic string.
        // However, diagnostic.toString() may contain "[unchecked]" even though getMessage() does
        // not.
        // Since we want to match the error messages reported by javac exactly, we must parse.
        // diagnostic.getCode() returns "compiler.warn.prob.found.req" for "[unchecked]" messages,
        // but not clear how to map from one to the other.
        IPair<String, Path> trimmed = formatJavaxToolString(diagnosticString);
        return fromPatternMatching(
                DIAGNOSTIC_PATTERN,
                DIAGNOSTIC_WARNING_PATTERN,
                trimmed.second,
                null,
                trimmed.first);
    }

    /**
     * Instantiate the diagnostic via pattern-matching against patterns.
     *
     * @param diagnosticPattern a pattern that matches any diagnostic
     * @param warningPattern a pattern that matches a warning diagnostic
     * @param file the test file
     * @param lineNumber the line number
     * @param diagnosticString the string to parse
     * @return a diagnostic parsed from the given string
     */
    @SuppressWarnings("nullness") // TODO: regular expression group access
    protected static TestDiagnostic fromPatternMatching(
            Pattern diagnosticPattern,
            Pattern warningPattern,
            Path file,
            @Nullable Long lineNumber,
            String diagnosticString) {
        final DiagnosticKind kind;
        final String message;
        final boolean isFixable;
        long lineNo = -1;

        if (lineNumber != null) {
            lineNo = lineNumber;
        }

        Matcher diagnosticMatcher = diagnosticPattern.matcher(diagnosticString);
        if (diagnosticMatcher.matches()) {
            IPair<DiagnosticKind, Boolean> categoryToFixable =
                    parseCategoryString(diagnosticMatcher.group("kind"));
            kind = categoryToFixable.first;
            isFixable = categoryToFixable.second;
            message = diagnosticMatcher.group("message").trim();
            if (lineNumber == null && diagnosticMatcher.group("linenogroup") != null) {
                lineNo = Long.parseLong(diagnosticMatcher.group("lineno"));
            }
        } else {
            Matcher warningMatcher = warningPattern.matcher(diagnosticString);
            if (warningMatcher.matches()) {
                kind = DiagnosticKind.Warning;
                isFixable = false;
                message = warningMatcher.group("message").trim();
                if (lineNumber == null && diagnosticMatcher.group("linenogroup") != null) {
                    lineNo = Long.parseLong(diagnosticMatcher.group("lineno"));
                }
            } else if (diagnosticString.startsWith("warning:")) {
                kind = DiagnosticKind.Warning;
                isFixable = false;
                message = diagnosticString.substring("warning:".length()).trim();
                if (lineNumber != null) {
                    lineNo = lineNumber;
                } else {
                    lineNo = 0;
                }
            } else {
                kind = DiagnosticKind.Other;
                isFixable = false;
                message = diagnosticString;
                // this should only happen if we are parsing a Java Diagnostic from the compiler
                // that we did do not handle
                if (lineNumber == null) {
                    lineNo = -1;
                }
            }
        }

        // Check if the message matches detailed message format.
        // Trim the message to remove leading/trailing whitespace.
        // Keep separator in sync with SourceChecker.DETAILS_SEPARATOR.
        String[] diagnosticStrings =
                Arrays.stream(message.split(" \\$\\$ ")).map(String::trim).toArray(String[]::new);
        if (diagnosticStrings.length > 1) {
            // See SourceChecker.detailedMsgTextPrefix.
            // The parts of the detailed message are:

            // (1) message key;
            String messageKey = diagnosticStrings[0];

            // (2) number of additional tokens, and those tokens; this depends on the error message,
            // and an example is the found and expected types;
            int numAdditionalTokens = Integer.parseInt(diagnosticStrings[1]);
            int lastAdditionalToken = 2 + numAdditionalTokens;
            List<String> additionalTokens =
                    Arrays.asList(diagnosticStrings).subList(2, lastAdditionalToken);

            // (3) the diagnostic position, given by the format (startPosition, endPosition);
            String pairParens = diagnosticStrings[lastAdditionalToken];
            // remove the leading and trailing parentheses and spaces
            String pair = pairParens.substring(2, pairParens.length() - 2);
            String[] diagPositionString = pair.split(", ");
            long startPosition = Long.parseLong(diagPositionString[0]);
            long endPosition = Long.parseLong(diagPositionString[1]);

            // (4) the human-readable diagnostic message.
            String readableMessage = diagnosticStrings[lastAdditionalToken + 1];

            return new DetailedTestDiagnostic(
                    file,
                    lineNo,
                    kind,
                    messageKey,
                    additionalTokens,
                    startPosition,
                    endPosition,
                    readableMessage,
                    isFixable);
        }

        return new TestDiagnostic(file, lineNo, kind, message, isFixable);
    }

    /**
     * Given a javax diagnostic, return a pair of (trimmed, file), where "trimmed" is the message
     * without the leading filename and the file path. As an example: "foo/bar/Baz.java:49: My error
     * message" is turned into {@code IPair.of(":49: My error message", Path("foo/bar/Baz.java"))}.
     * If the file path cannot be determined, it uses {@code ""}. This is necessary to make writing
     * the expected warnings easy.
     *
     * @param original a javax diagnostic
     * @return the diagnostic, split into message and file
     */
    public static IPair<String, Path> formatJavaxToolString(String original) {
        String firstline;
        // In TestDiagnostic we manually check for "\r\n" and "\n". Here, we only use
        // `firstline` to find the file name. Using the system line separator is not
        // problem here, it seems.
        int lineSepPos = original.indexOf(System.lineSeparator());
        if (lineSepPos != -1) {
            firstline = original.substring(0, lineSepPos);
        } else {
            firstline = original;
        }

        String trimmed;
        Path file;
        int extensionPos = firstline.indexOf(".java:");
        if (extensionPos != -1) {
            file = Paths.get(firstline.substring(0, extensionPos + 5).trim());
            trimmed = original.substring(extensionPos + 5).trim();
        } else {
            // Important to use "" to make input of expected warnings easy.
            // For an example, see file
            // ./checker/tests/nullness-stubfile/NullnessStubfileMerge.java
            file = Paths.get("");
            trimmed = original;
        }

        return IPair.of(trimmed, file);
    }

    /**
     * Given a category string that may be prepended with "fixable-", return the category enum that
     * corresponds with the category and whether or not it is a isFixable error.
     *
     * @param category a category string
     * @return the corresponding diagnostic kind and whether it is fixable
     */
    private static IPair<DiagnosticKind, Boolean> parseCategoryString(String category) {
        String fixable = "fixable-";
        boolean isFixable = category.startsWith(fixable);
        if (isFixable) {
            category = category.substring(fixable.length());
        }
        DiagnosticKind categoryEnum = DiagnosticKind.fromParseString(category);
        if (categoryEnum == null) {
            throw new Error("Unparsable category: " + category);
        }

        return IPair.of(categoryEnum, isFixable);
    }

    /**
     * Return true if this line in a Java file indicates an expected diagnostic that might be
     * continued on the next line.
     *
     * @param originalLine the input line
     * @return whether the diagnostic might be continued on the next line
     */
    public static boolean isJavaDiagnosticLineStart(String originalLine) {
        String trimmedLine = originalLine.trim();
        return trimmedLine.startsWith("// ::") || trimmedLine.startsWith("// warning:");
    }

    /**
     * Convert an end-of-line diagnostic message to a beginning-of-line one. Returns the argument
     * unchanged if it does not contain an end-of-line diagnostic message.
     *
     * <p>Most diagnostics in Java files start at the beginning of a line. Occasionally, javac
     * issues a warning about implicit code, such as an implicit constructor, on the line
     * <em>immediately after</em> a curly brace. The only place to put the expected diagnostic
     * message is on the line with the curly brace.
     *
     * <p>This implementation replaces "{ // ::" by "// ::", converting the end-of-line diagnostic
     * message to a beginning-of-line one that the rest of the code can handle. It is rather
     * specific (to avoid false positive matches, such as when "// ::" is commented out in source
     * code). It could be extended in the future if such an extension is necessary.
     */
    public static String handleEndOfLineJavaDiagnostic(String originalLine) {
        int curlyIndex = originalLine.indexOf("{ // ::");
        if (curlyIndex == -1) {
            return originalLine;
        } else {
            return originalLine.substring(curlyIndex + 2);
        }
    }

    /** Return true if this line in a Java file continues an expected diagnostic. */
    @EnsuresNonNullIf(result = true, expression = "#1")
    public static boolean isJavaDiagnosticLineContinuation(@Nullable String originalLine) {
        if (originalLine == null) {
            return false;
        }
        String trimmedLine = originalLine.trim();
        // Unlike with errors, there is no logic elsewhere for splitting multiple "warning:"s.  So,
        // avoid concatenating them.  Also, each one must begin a line.  They are allowed to wrap to
        // the next line, though.
        return trimmedLine.startsWith("// ") && !trimmedLine.startsWith("// warning:");
    }

    /**
     * Return the continuation part. The argument is such that {@link
     * #isJavaDiagnosticLineContinuation} returns true.
     */
    public static String continuationPart(String originalLine) {
        return originalLine.trim().substring(2).trim();
    }

    /**
     * Convert a line in a Java source file to a TestDiagnosticLine.
     *
     * <p>The input {@code line} is possibly the concatenation of multiple source lines, if the
     * diagnostic was split across lines in the source code.
     */
    public static TestDiagnosticLine fromJavaSourceLine(
            String filename, String line, long lineNumber) {
        String trimmedLine = line.trim();
        long errorLine = lineNumber + 1;

        if (trimmedLine.startsWith("// ::")) {
            String restOfLine = trimmedLine.substring(5); // drop the "// ::"
            String[] diagnosticStrs = restOfLine.split("::");
            List<TestDiagnostic> diagnostics =
                    CollectionsPlume.mapList(
                            (String diagnostic) ->
                                    fromJavaFileComment(filename, errorLine, diagnostic),
                            diagnosticStrs);
            return new TestDiagnosticLine(
                    filename, errorLine, line, Collections.unmodifiableList(diagnostics));
        } else if (trimmedLine.startsWith("// warning:")) {
            // This special diagnostic does not expect a line number nor a file name
            String diagnosticString = trimmedLine.substring(2);
            TestDiagnostic diagnostic = fromJavaFileComment("", -1, diagnosticString);
            return new TestDiagnosticLine("", -1, line, Collections.singletonList(diagnostic));
        } else if (trimmedLine.startsWith("//::")) {
            TestDiagnostic diagnostic =
                    new TestDiagnostic(
                            Paths.get(filename),
                            lineNumber,
                            DiagnosticKind.Error,
                            "Use \"// ::\", not \"//::\"",
                            false);
            return new TestDiagnosticLine(
                    filename, lineNumber, line, Collections.singletonList(diagnostic));
        } else {
            // It's a bit gross to create empty diagnostics (returning null might be more
            // efficient), but they will be filtered out later.
            return new TestDiagnosticLine(filename, errorLine, line, Collections.emptyList());
        }
    }

    /** Convert a line in a DiagnosticFile to a TestDiagnosticLine. */
    public static TestDiagnosticLine fromDiagnosticFileLine(String diagnosticLine) {
        String trimmedLine = diagnosticLine.trim();
        if (trimmedLine.startsWith("#") || trimmedLine.isEmpty()) {
            return new TestDiagnosticLine("", -1, diagnosticLine, Collections.emptyList());
        }

        TestDiagnostic diagnostic = fromDiagnosticFileString(diagnosticLine);
        return new TestDiagnosticLine(
                "", diagnostic.getLineNumber(), diagnosticLine, Arrays.asList(diagnostic));
    }

    /**
     * Convert a list of compiler diagnostics into test diagnostics.
     *
     * @param javaxDiagnostics the list of compiler diagnostics
     * @return the corresponding test diagnostics
     */
    public static Set<TestDiagnostic> fromJavaxToolsDiagnosticList(
            List<Diagnostic<? extends JavaFileObject>> javaxDiagnostics) {
        Set<TestDiagnostic> diagnostics = new LinkedHashSet<>(javaxDiagnostics.size());

        for (Diagnostic<? extends JavaFileObject> diagnostic : javaxDiagnostics) {
            // See fromJavaxToolsDiagnostic as to why we use diagnostic.toString rather
            // than convert from the diagnostic itself
            String diagnosticString = diagnostic.toString();

            // suppress Xlint warnings
            if (diagnosticString.contains("uses unchecked or unsafe operations.")
                    || diagnosticString.contains("Recompile with -Xlint:unchecked for details.")
                    || diagnosticString.endsWith(" declares unsafe vararg methods.")
                    || diagnosticString.contains("Recompile with -Xlint:varargs for details.")) {
                continue;
            }

            diagnostics.add(fromJavaxToolsDiagnostic(diagnosticString));
        }

        return diagnostics;
    }

    /**
     * Converts the given diagnostics to strings (as they would appear in a source file
     * individually).
     *
     * @param diagnostics a list of diagnostics
     * @return a list of the diagnastics as they would appear in a source file
     */
    public static List<String> diagnosticsToString(List<TestDiagnostic> diagnostics) {
        return CollectionsPlume.mapList(TestDiagnostic::toString, diagnostics);
    }
}
