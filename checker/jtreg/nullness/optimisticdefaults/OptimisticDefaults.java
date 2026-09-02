/*
 * @test
 *
 * @summary Test optimistic defaults for bytecode, bounds, and option validation.
 * @compile -proc:none ../optimisticdefaultslib/Lib.java
 * @compile/fail/ref=NoDefaults.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker OptimisticDefaults.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=bytecode OptimisticDefaults.java
 * @compile/fail/ref=ConservativeDefaults.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode OptimisticDefaults.java
 * @compile/fail/ref=InvalidOptimisticOption.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=btyecode OptimisticDefaults.java
 * @compile/fail/ref=ConflictingOptimisticOption.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=source,-source OptimisticDefaults.java
 * @compile/fail/ref=ConflictingDefaultModes.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=bytecode -AuseConservativeDefaultsForUncheckedCode=bytecode OptimisticDefaults.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import optimisticdefaultslib.Lib;

public class OptimisticDefaults {
    void calls() {
        Lib.setObject(null);
        Lib.getObject().toString();
    }

    void bounds(Lib<@Nullable Object> nullableArgument, Lib<@NonNull Object> nonNullArgument) {
        Lib.upper(nullableArgument);
        Lib.lower(nullableArgument);
        Lib.lower(nonNullArgument);
    }
}
