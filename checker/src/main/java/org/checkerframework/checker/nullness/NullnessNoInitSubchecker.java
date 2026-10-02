package org.checkerframework.checker.nullness;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;

import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.checker.initialization.InitializationFieldAccessSubchecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedModes;
import org.checkerframework.framework.source.SupportedOptions;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

/**
 * The subchecker of the {@link NullnessChecker} which actually checks {@link NonNull} and related
 * qualifiers.
 *
 * <p>The {@link NullnessChecker} uses this checker as the target (see {@link
 * InitializationChecker#getTargetCheckerClass()}) for its initialization type system.
 *
 * <p>You can use the following {@link SuppressWarnings} prefixes with this checker:
 *
 * <ul>
 *   <li>{@code @SuppressWarnings("nullness")} suppresses warnings from the Nullness,
 *       Initialization, and KeyFor Checkers
 *   <li>{@code @SuppressWarnings("nullnessinitialization")} suppresses warnings from the Nullness
 *       and Initialization Checkers only, warnings from the KeyFor Checker are not suppressed
 *   <li>{@code @SuppressWarnings("nullnesskeyfor")} suppresses warnings from the Nullness and
 *       KeyFor Checkers only, warnings from the Initialization Checker are not suppressed
 *       {@code @SuppressWarnings("nullnessnoinit")} has the same effect as
 *       {@code @SuppressWarnings("nullnesskeyfor")}
 *   <li>{@code @SuppressWarnings("nullnessonly")} suppresses warnings from the Nullness Checker
 *       only, warnings from the Initialization and KeyFor Checkers are not suppressed
 * </ul>
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
    NullnessChecker.LINT_MONOTONICNONNULLONSTATIC,
})
@SupportedOptions({
    "assumeKeyFor",
    "jspecifyNullMarkedAlias",
    "jspecifyUnrecognizedLocations",
    "conservativeArgumentNullnessAfterInvocation"
})
@SupportedModes(NullnessChecker.MODE_JSPECIFY)
@StubFiles({"java-lang-classfile.astub", "junit-assertions.astub", "log4j.astub"})
public class NullnessNoInitSubchecker extends BaseTypeChecker {

    /** Default constructor for NonNullChecker. */
    public NullnessNoInitSubchecker() {}

    @Override
    protected void addOptionsForMode(String mode, Map<String, String> activeOptions) {
        super.addOptionsForMode(mode, activeOptions);
        switch (mode) {
            case NullnessChecker.MODE_JSPECIFY:
                activeOptions.putIfAbsent("onlyAnnotatedFor", null);
                // Already the default; named here so the mode states the behavior it relies on.
                activeOptions.putIfAbsent("jspecifyNullMarkedAlias", "true");
                activeOptions.putIfAbsent("assumeKeyFor", null);
                activeOptions.putIfAbsent("jspecifyUnrecognizedLocations", null);
                activeOptions.putIfAbsent("assumePure", null);
                // Not putIfAbsent: a bare -AstoreInBytecode maps to null, which putIfAbsent would
                // overwrite, turning storage off where the flag means to leave it on.
                if (!activeOptions.containsKey("storeInBytecode")) {
                    activeOptions.put("storeInBytecode", "false");
                }
                activeOptions.putIfAbsent("assumeAssertions", "enabled");
                // Conservative defaults written on the command line replace the mode's permissive
                // ones, which would otherwise conflict with them.
                if (!activeOptions.containsKey("useConservativeDefaultsForUncheckedCode")) {
                    activeOptions.putIfAbsent(
                            "usePermissiveDefaultsForUncheckedCode", "source,bytecode");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public NullnessNoInitAnnotatedTypeFactory getTypeFactory() {
        return (NullnessNoInitAnnotatedTypeFactory) super.getTypeFactory();
    }

    @Override
    protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
        Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
        if (!hasOptionNoSubcheckers("assumeKeyFor")) {
            checkers.add(KeyForSubchecker.class);
        }
        checkers.add(InitializationFieldAccessSubchecker.class);
        return checkers;
    }

    @Override
    public NavigableSet<String> getSuppressWarningsPrefixes() {
        NavigableSet<String> result = super.getSuppressWarningsPrefixes();
        result.add("nullnessonly");
        result.add("nullnesskeyfor");
        result.add("nullnessinitialization");
        return result;
    }

    @Override
    protected String getWarningMessagePrefix() {
        return "nullness";
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new NullnessNoInitVisitor(this);
    }

    // The NullnessNoInitChecker should also skip defs skipped by the NullnessChecker

    @Override
    public boolean shouldSkipDefs(ClassTree tree) {
        return super.shouldSkipDefs(tree)
                || (parentChecker != null && parentChecker.shouldSkipDefs(tree));
    }

    @Override
    public boolean shouldSkipDefs(MethodTree tree) {
        return super.shouldSkipDefs(tree)
                || (parentChecker != null && parentChecker.shouldSkipDefs(tree));
    }
}
