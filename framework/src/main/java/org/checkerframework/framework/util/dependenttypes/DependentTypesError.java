package org.checkerframework.framework.util.dependenttypes;

import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.javacutil.BugInCF;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for creating dependent type annotation error strings.
 *
 * <p><b>IMPORTANT:</b> This is not an Exception. It is a regular class that is returned, not
 * thrown. The errors are not thrown so that they are only reported once rather than every time the
 * annotation is parsed. See {@link DependentTypesHelper} for more details.
 */
public class DependentTypesError {

    /// Static fields

    /** How elements of this class are formatted. */
    @SuppressWarnings("InlineFormatString") // https://github.com/google/error-prone/issues/1650
    private static final String FORMAT_STRING = "[error for expression: %s; error: %s]";

    /** Regular expression for unparsing string representations of this class (gross). */
    private static final Pattern ERROR_PATTERN =
            Pattern.compile("\\[error for expression: (.*); error: ([\\s\\S]*)\\]");

    /**
     * Returns whether or not the given expression string is an error. That is, whether it is a
     * string that was generated by this class.
     *
     * @param expression expression string to test
     * @return whether or not the given expressions string is an error
     */
    public static boolean isExpressionError(String expression) {
        return expression.startsWith("[error");
    }

    /** How to format warnings about use of formal parameter name. */
    public static final @Format({ConversionCategory.INT, ConversionCategory.GENERAL}) String
            FORMAL_PARAM_NAME_STRING = "Use \"#%d\" rather than \"%s\"";

    /** Matches warnings about use of formal parameter name. */
    private static final Pattern FORMAL_PARAM_NAME_PATTERN =
            Pattern.compile(
                    "^'([a-zA-Z_$][a-zA-Z0-9_$]*)' because (Use \"#\\d+\" rather than \"\\1\")$");

    /// Instance fields

    /** The expression that is unparsable or otherwise problematic. */
    public final String expression;

    /** An error message about that expression. */
    public final String error;

    /// Constructors and methods

    /**
     * Create a DependentTypesError for the given expression and error message.
     *
     * @param expression the incorrect Java expression
     * @param error an error message about the expression
     */
    public DependentTypesError(String expression, String error) {
        this.expression = expression;
        this.error = error;
    }

    /**
     * Create a DependentTypesError for the given expression and exception.
     *
     * @param expression the incorrect Java expression
     * @param e wraps an error message about the expression
     */
    public DependentTypesError(String expression, JavaExpressionParseException e) {
        this.expression = expression;
        this.error = e.getDiagMessage().getArgs()[0].toString();
    }

    /**
     * Create a DependentTypesError by parsing a printed one.
     *
     * @param formattedError the toString() representation of a DependentTypesError
     */
    public static DependentTypesError unparse(String formattedError) {
        Matcher matcher = ERROR_PATTERN.matcher(formattedError);
        if (matcher.matches()) {
            assert matcher.groupCount() == 2;
            return new DependentTypesError(matcher.group(1), matcher.group(2));
        } else {
            throw new BugInCF("Cannot unparse: " + formattedError);
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DependentTypesError that = (DependentTypesError) o;

        return expression.equals(that.expression) && error.equals(that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, error);
    }

    @Override
    public String toString() {
        return String.format(FORMAT_STRING, expression, error);
    }

    /**
     * Like toString, but uses better formatting sometimes. Use this only for the final output,
     * because of the design that hides error messages in toString().
     */
    @SuppressWarnings("nullness:return") // regex groups always match text
    public String format() {
        Matcher m = FORMAL_PARAM_NAME_PATTERN.matcher(error);
        if (m.matches()) {
            return m.group(2);
        }
        return toString();
    }
}
