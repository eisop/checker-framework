package org.checkerframework.checker.nullness;

import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedLintOptions;

import java.util.Set;

import javax.annotation.processing.SupportedOptions;

/**
 * An implementation of the nullness type-system, parameterized by an initialization type-system for
 * safe initialization. It uses freedom-before-commitment, augmented by type frames (which are
 * crucial to obtain acceptable precision), as its initialization type system.
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@SupportedLintOptions({
    NullnessChecker.LINT_NOINITFORMONOTONICNONNULL,
    NullnessChecker.LINT_REDUNDANTNULLCOMPARISON,
    // Temporary option to forbid non-null array component types, which is allowed by default.
    // Forbidding is sound and will eventually be the default.
    // Allowing is unsound, as described in Section 3.3.4, "Nullness and arrays":
    //     https://eisop.github.io/cf/manual/#nullness-arrays
    // It is the default temporarily, until we improve the analysis to reduce false positives or we
    // learn what advice to give programmers about avoid false positive warnings.
    // See issue #986: https://github.com/typetools/checker-framework/issues/986
    "soundArrayCreationNullness",
    // Old name for soundArrayCreationNullness, for backward compatibility; remove in January 2021.
    "forbidnonnullarraycomponents",
    NullnessChecker.LINT_TRUSTARRAYLENZERO,
    NullnessChecker.LINT_PERMITCLEARPROPERTY,
})
@SupportedOptions({
    "assumeKeyFor",
    "assumeInitialized",
    "jspecifyNullMarkedAlias",
    "conservativeArgumentNullnessAfterInvocation"
})
@StubFiles({"junit-assertions.astub"})
public class NullnessChecker extends BaseTypeChecker {

    /** Should we be strict about initialization of {@link MonotonicNonNull} variables? */
    public static final String LINT_NOINITFORMONOTONICNONNULL = "noInitForMonotonicNonNull";

    /** Default for {@link #LINT_NOINITFORMONOTONICNONNULL}. */
    public static final boolean LINT_DEFAULT_NOINITFORMONOTONICNONNULL = false;

    /**
     * Warn about redundant comparisons of an expression with {@code null}, if the expression is
     * known to be non-null.
     */
    public static final String LINT_REDUNDANTNULLCOMPARISON = "redundantNullComparison";

    /** Default for {@link #LINT_REDUNDANTNULLCOMPARISON}. */
    public static final boolean LINT_DEFAULT_REDUNDANTNULLCOMPARISON = false;

    /**
     * Should the Nullness Checker unsoundly trust {@code @ArrayLen(0)} annotations to improve
     * handling of {@link java.util.Collection#toArray()} by {@link CollectionToArrayHeuristics}?
     */
    public static final String LINT_TRUSTARRAYLENZERO = "trustArrayLenZero";

    /** Default for {@link #LINT_TRUSTARRAYLENZERO}. */
    public static final boolean LINT_DEFAULT_TRUSTARRAYLENZERO = false;

    /**
     * If true, client code may clear system properties. If false (the default), some calls to
     * {@code System.getProperty} are refined to return @NonNull.
     */
    public static final String LINT_PERMITCLEARPROPERTY = "permitClearProperty";

    /** Default for {@link #LINT_PERMITCLEARPROPERTY}. */
    public static final boolean LINT_DEFAULT_PERMITCLEARPROPERTY = false;

    /** Default constructor for NullnessChecker. */
    public NullnessChecker() {}

    @Override
    protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
        if (!hasOptionNoSubcheckers("assumeKeyFor")) {
            checkers.add(KeyForSubchecker.class);
        }
        if (!hasOptionNoSubcheckers("assumeInitialized")) {
            checkers.add(InitializationChecker.class);
        } else {
            checkers.add(NonNullSubchecker.class);
        }
        return checkers;
    }
}
