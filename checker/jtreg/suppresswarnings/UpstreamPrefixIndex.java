import org.checkerframework.common.value.qual.IntVal;

/**
 * Tests that a checker's default SuppressWarnings prefix also covers findings from checkers it runs
 * as (possibly indirect) subcheckers -- see {@code
 * SourceChecker#getStandardSuppressWarningsPrefixes} and {@code
 * SourceChecker#getUpstreamCheckerNames}.
 *
 * <p>The Value Checker runs as a subchecker of the Index Checker. "index" is neither the Value
 * Checker's own name ("value") nor a substring of the message key ("assignment.type.incompatible"),
 * and {@code -ArequirePrefixInWarningSuppressions} disables the partial-message-key suppression
 * fallback, so {@code suppressed()} compiling cleanly is possible only because the Value Checker's
 * default prefixes now include "index", derived from {@code IndexChecker} being one of its upstream
 * checkers.
 */
public class UpstreamPrefixIndex {

    @SuppressWarnings("index")
    static void suppressed() {
        @IntVal(1) int x = 2;
    }

    static void notSuppressed() {
        @IntVal(1) int x = 2;
    }
}
