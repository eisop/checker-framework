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
@StubFiles({"junit-assertions.astub"})
public class NullnessNoInitSubchecker extends BaseTypeChecker {

    /** Default constructor for NonNullChecker. */
    public NullnessNoInitSubchecker() {}

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
        result.add("nullness");
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
        return super.shouldSkipDefs(tree) || parentChecker.shouldSkipDefs(tree);
    }

    @Override
    public boolean shouldSkipDefs(MethodTree tree) {
        return super.shouldSkipDefs(tree) || parentChecker.shouldSkipDefs(tree);
    }
}
