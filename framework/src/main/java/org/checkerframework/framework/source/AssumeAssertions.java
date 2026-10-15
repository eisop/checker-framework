package org.checkerframework.framework.source;

/**
 * What to assume about whether Java {@code assert} statements are executed at run time. The {@code
 * -AassumeAssertions} command-line option selects one; see {@link
 * SourceChecker#getAssumeAssertions()}.
 */
public enum AssumeAssertions {
    /** Assume assertions are enabled, as if Java is run with {@code -enableassertions}. */
    ENABLED,
    /** Assume assertions are disabled, as if Java is run with {@code -disableassertions}. */
    DISABLED,
    /** Make neither assumption, and account for both cases. This is the default. */
    NEITHER
}
