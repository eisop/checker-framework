package org.checkerframework.checker.nullness;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;

import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.checker.initialization.InitializationFieldAccessSubchecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;

import java.util.NavigableSet;
import java.util.Set;

/**
 * The subchecker of the {@link NullnessChecker} which actually checks {@link NonNull} and related
 * qualifiers.
 *
 * <p>The {@link NullnessChecker} uses this checker as the target (see {@link
 * InitializationChecker#getTargetCheckerClass()}) for its initialization type system.
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
    protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
        if (!hasOption("assumeKeyFor")) {
            checkers.add(KeyForSubchecker.class);
        }
        checkers.add(InitializationFieldAccessSubchecker.class);
        return checkers;
    }

    @Override
    public NavigableSet<String> getSuppressWarningsPrefixes() {
        NavigableSet<String> result = super.getSuppressWarningsPrefixes();
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
