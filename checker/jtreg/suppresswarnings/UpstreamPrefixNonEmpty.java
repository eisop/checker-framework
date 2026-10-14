import java.util.OptionalDouble;

/**
 * Tests that a checker's default SuppressWarnings prefix also covers findings from checkers it runs
 * as (possibly indirect) subcheckers -- see {@code
 * SourceChecker#getStandardSuppressWarningsPrefixes} and {@code
 * SourceChecker#getUpstreamCheckerNames}.
 *
 * <p>The OptionalImplChecker runs as a subchecker of the Non-Empty Checker, which in turn runs as a
 * subchecker of the top-level Optional Checker. "nonempty" is neither OptionalImplChecker's own
 * name ("optionalimpl") nor a substring of the message key ("optional.field"), and {@code
 * -ArequirePrefixInWarningSuppressions} disables the partial-message-key suppression fallback, so
 * {@code Suppressed} compiling with no warning is possible only because OptionalImplChecker's
 * default prefixes now include "nonempty", derived from {@code NonEmptyChecker} being one of its
 * upstream checkers.
 */
public class UpstreamPrefixNonEmpty {

    @SuppressWarnings("nonempty")
    static class Suppressed {
        OptionalDouble aField;
    }

    static class NotSuppressed {
        OptionalDouble aField;
    }
}
